package com.competition.energycontrollerplatform.integration.http.client;

import com.competition.energycontrollerplatform.integration.config.VendorApiProperties;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Client for the authenticated API used by China Meter Reading Online V5. */
@Component
public class JiexianV5Client {
    private static final Logger log = LoggerFactory.getLogger(JiexianV5Client.class);
    private static final String CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e";
    private static final String PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==";
    private static final char[] RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final VendorApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<Integer, String> accessTokens = new ConcurrentHashMap<>();
    private volatile Path tessdataDirectory;

    public JiexianV5Client(VendorApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.getJiexian().isV5Enabled();
    }

    public List<JiexianDTO.DataValueItem> fetchRecentReadings() {
        if (!isEnabled()) return List.of();
        List<Account> accounts = configuredAccounts();
        if (accounts.isEmpty()) {
            log.warn("Jiexian V5 is enabled but no account credentials are configured");
            return List.of();
        }
        List<JiexianDTO.DataValueItem> result = new ArrayList<>();
        for (Account account : accounts) {
            try {
                result.addAll(fetchForAccount(account));
            } catch (Exception e) {
                log.error("Jiexian V5 account {} sync failed: {}", account.slot(), safeMessage(e));
            }
        }
        return result;
    }

    private List<JiexianDTO.DataValueItem> fetchForAccount(Account account) throws Exception {
        String token = accessTokens.get(account.slot());
        if (!StringUtils.hasText(token)) token = login(account);
        try {
            return fetchPages(token);
        } catch (UnauthorizedException e) {
            accessTokens.remove(account.slot());
            return fetchPages(login(account));
        }
    }

    private List<JiexianDTO.DataValueItem> fetchPages(String token) throws Exception {
        VendorApiProperties.Jiexian cfg = properties.getJiexian();
        int pageSize = Math.max(1, Math.min(cfg.getV5PageSize(), 200));
        LocalDate end = LocalDate.now();
        LocalDate begin = end.minusDays(Math.max(1, cfg.getV5LookbackDays()) - 1L);
        List<JiexianDTO.DataValueItem> result = new ArrayList<>();
        for (int page = 1; page <= 1000; page++) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("pageNum", page);
            request.put("pageSize", pageSize);
            request.put("beginTime", begin.format(DateTimeFormatter.ISO_DATE));
            request.put("endTime", end.format(DateTimeFormatter.ISO_DATE));
            request.put("searchType", "3");
            JsonNode body = postJson("/mr/hisdata/list", request, token);
            JsonNode rows = body.path("rows");
            if (!rows.isArray() && body.path("data").isObject()) rows = body.path("data").path("rows");
            if (!rows.isArray() || rows.isEmpty()) break;
            for (JsonNode row : rows) result.add(toReading(row));
            long total = body.path("total").asLong(body.path("data").path("total").asLong(-1));
            if (rows.size() < pageSize || total >= 0 && (long) page * pageSize >= total) break;
        }
        log.info("Jiexian V5 fetched {} readings", result.size());
        return result;
    }

    private String login(Account account) throws Exception {
        Captcha captcha = fetchCaptcha();
        String answer = solveCaptcha(captcha.image());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("username", account.username());
        request.put("password", account.password());
        request.put("code", answer);
        request.put("uuid", captcha.uuid());
        request.put("clientId", CLIENT_ID);
        request.put("grantType", "password");

        String aesKey = randomKey();
        String encryptedBody = encryptAes(objectMapper.writeValueAsString(request), aesKey);
        HttpHeaders headers = commonHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("isEncrypt", "true");
        headers.set("encrypt-key", encryptRsa(Base64.getEncoder().encodeToString(aesKey.getBytes(StandardCharsets.UTF_8))));
        ResponseEntity<String> response = restTemplate.exchange(url("/auth/login"), HttpMethod.POST,
                new HttpEntity<>(encryptedBody, headers), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.path("code").asInt(-1) != 200) throw new IllegalStateException("login status=" + root.path("code").asText());
        JsonNode data = root.path("data");
        String token = firstText(data, "accessToken", "access_token", "token");
        if (!StringUtils.hasText(token) && data.isTextual()) token = data.asText();
        if (!StringUtils.hasText(token)) throw new IllegalStateException("login response contained no access token");
        accessTokens.put(account.slot(), token);
        log.info("Jiexian V5 account {} login succeeded", account.slot());
        return token;
    }

    private Captcha fetchCaptcha() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/auth/code"), HttpMethod.GET,
                new HttpEntity<>(commonHeaders()), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");
        if (!data.path("captchaEnabled").asBoolean(true)) return new Captcha("", null);
        String uuid = data.path("uuid").asText();
        String image = data.path("img").asText();
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(image)) throw new IllegalStateException("invalid captcha response");
        int comma = image.indexOf(',');
        if (comma >= 0) image = image.substring(comma + 1);
        return new Captcha(uuid, Base64.getDecoder().decode(image));
    }

    private String solveCaptcha(byte[] bytes) throws Exception {
        if (bytes == null) return "";
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        BufferedImage clean = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            Color c = new Color(source.getRGB(x, y), true);
            int min = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
            clean.setRGB(x, y, min < 190 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
        }
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolveTessdataDirectory().toString());
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(7);
        tesseract.setVariable("tessedit_char_whitelist", "0123456789+-xX*=");
        String expression = tesseract.doOCR(clean).replaceAll("\\s+", "").replace("=", "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)([+\\-xX*])(\\d+)").matcher(expression);
        if (!matcher.find()) throw new IllegalStateException("captcha could not be recognized");
        int left = Integer.parseInt(matcher.group(1));
        int right = Integer.parseInt(matcher.group(3));
        return switch (matcher.group(2)) { case "+" -> String.valueOf(left + right); case "-" -> String.valueOf(left - right); default -> String.valueOf(left * right); };
    }

    private JsonNode postJson(String path, Object body, String token) throws Exception {
        HttpHeaders headers = commonHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("code").asInt(200) == 401) throw new UnauthorizedException();
            return root;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 401) throw new UnauthorizedException();
            throw e;
        }
    }

    private JiexianDTO.DataValueItem toReading(JsonNode row) {
        JiexianDTO.DataValueItem item = new JiexianDTO.DataValueItem();
        item.setMeteraddress(text(row, "meteraddress"));
        item.setDataDt(text(row, "dataDt"));
        item.setLoadDt(text(row, "loadDt"));
        if (row.hasNonNull("dataValue0")) try { item.setDataValue0(new BigDecimal(row.path("dataValue0").asText())); } catch (NumberFormatException ignored) {}
        item.setChargeCustomerNo(text(row, "chargeCustomerNo"));
        item.setCustomerName(text(row, "customerName"));
        item.setAddress(text(row, "address"));
        item.setMeterCaliber(text(row, "meterCaliber"));
        item.setSignalStrength(text(row, "signalStrength"));
        item.setBatteryVoltage(text(row, "batteryVoltage"));
        item.setStatusName(text(row, "statusName"));
        item.setFacCode(text(row, "facCode"));
        return item;
    }

    private HttpHeaders commonHeaders() { HttpHeaders h = new HttpHeaders(); h.set("clientid", CLIENT_ID); h.setAccept(List.of(MediaType.APPLICATION_JSON)); return h; }
    private String url(String path) { String base = properties.getJiexian().getV5BaseUrl(); return base.replaceAll("/+$", "") + path; }
    private String randomKey() { StringBuilder s = new StringBuilder(32); for (int i=0;i<32;i++) s.append(RANDOM_CHARS[secureRandom.nextInt(RANDOM_CHARS.length)]); return s.toString(); }
    private String encryptAes(String value, String key) throws Exception { Cipher c=Cipher.getInstance("AES/ECB/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"AES")); return Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
    private String encryptRsa(String value) throws Exception { PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY))); Cipher c=Cipher.getInstance("RSA/ECB/PKCS1Padding"); c.init(Cipher.ENCRYPT_MODE,key); return Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
    private List<Account> configuredAccounts() { VendorApiProperties.Jiexian c=properties.getJiexian(); List<Account> a=new ArrayList<>(); if(StringUtils.hasText(c.getV5Username1())&&StringUtils.hasText(c.getV5Password1()))a.add(new Account(1,c.getV5Username1(),c.getV5Password1())); if(StringUtils.hasText(c.getV5Username2())&&StringUtils.hasText(c.getV5Password2()))a.add(new Account(2,c.getV5Username2(),c.getV5Password2())); return a; }
    private String firstText(JsonNode node,String...names){for(String n:names){String v=node.path(n).asText();if(StringUtils.hasText(v))return v;}return null;}
    private String text(JsonNode node,String name){String v=node.path(name).asText(null);return StringUtils.hasText(v)?v:null;}
    private String safeMessage(Exception e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m.replaceAll("(?i)(bearer|token|password|cookie)[^,; ]*","[redacted]");}
    private synchronized Path resolveTessdataDirectory() throws Exception {
        if (tessdataDirectory != null) return tessdataDirectory;
        Path directory = Files.createTempDirectory("energy-jiexian-tessdata-");
        Path model = directory.resolve("eng.traineddata");
        try (InputStream input = new ClassPathResource("tessdata/eng.traineddata").getInputStream()) {
            Files.copy(input, model);
        }
        directory.toFile().deleteOnExit();
        model.toFile().deleteOnExit();
        tessdataDirectory = directory;
        return directory;
    }
    private record Account(int slot,String username,String password){}
    private record Captcha(String uuid,byte[] image){}
    private static class UnauthorizedException extends Exception {}
}
