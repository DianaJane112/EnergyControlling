package com.competition.energycontrollerplatform.integration.http.client;

import com.competition.energycontrollerplatform.integration.config.WaterMeterVendorConfigProvider;
import com.competition.energycontrollerplatform.integration.dto.JichengDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 积成水表 HTTP 客户端，用于档案同步。
 */
@Component
public class JichengWaterClient {

    private static final Logger log = LoggerFactory.getLogger(JichengWaterClient.class);

    private final RestTemplate restTemplate;
    private final WaterMeterVendorConfigProvider configProvider;

    public JichengWaterClient(WaterMeterVendorConfigProvider configProvider) {
        this.configProvider = configProvider;
        this.restTemplate = new RestTemplate();
    }

    /** 拉取积成项目当前水表读数。sign 是项目数据范围编码，不是网页登录凭证。 */
    public List<JichengDTO.ReadingItem> fetchMeterReadings() {
        WaterMeterVendorConfigProvider.JichengVendorCredentials credentials =
                configProvider.resolveJichengCredentials();
        if (!StringUtils.hasText(credentials.baseUrl())
                || !StringUtils.hasText(credentials.sign())) {
            log.warn("积成读数同步未配置 baseUrl/sign，跳过");
            return null;
        }

        String url = credentials.baseUrl() + "/GetMeterReading";
        JichengDTO.ApiRequest request = new JichengDTO.ApiRequest();
        request.setSign(credentials.sign());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<JichengDTO.MeterReadingResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request, headers),
                    JichengDTO.MeterReadingResponse.class);
            JichengDTO.MeterReadingResponse body = response.getBody();
            List<JichengDTO.ReadingItem> rows = body != null && body.getData() != null
                    ? body.getData().getAll() : null;
            log.info("积成 GetMeterReading 拉取成功, count={}", rows == null ? 0 : rows.size());
            return rows == null ? List.of() : rows;
        } catch (RestClientException e) {
            log.error("积成 GetMeterReading 请求失败: {}", e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> fetchMeterArchive(int page) {
        return fetchMeterArchive(page, null);
    }

    public List<Map<String, Object>> fetchMeterArchive(int page, Long vendorId) {
        WaterMeterVendorConfigProvider.JichengVendorCredentials credentials =
                configProvider.resolveJichengCredentials(vendorId);
        String baseUrl = credentials.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("积成 baseUrl 未配置，跳过表计档案同步");
            return null;
        }

        String url = baseUrl + "/meterTran?sign={sign}&page={page}";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            new ParameterizedTypeReference<>() {},
                            credentials.sign(), page);
            log.info("积成 meterTran page={} 拉取成功, count={} vendorId={}",
                    page, resp.getBody() != null ? resp.getBody().size() : 0, vendorId);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("积成 meterTran 请求失败 page={} vendorId={}: {}", page, vendorId, e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> fetchCustomerArchive(int page) {
        return fetchCustomerArchive(page, null);
    }

    public List<Map<String, Object>> fetchCustomerArchive(int page, Long vendorId) {
        WaterMeterVendorConfigProvider.JichengVendorCredentials credentials =
                configProvider.resolveJichengCredentials(vendorId);
        String baseUrl = credentials.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("积成 baseUrl 未配置，跳过客户档案同步");
            return null;
        }

        String url = baseUrl + "/customTran?sign={sign}&page={page}";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            new ParameterizedTypeReference<>() {},
                            credentials.sign(), page);
            log.info("积成 customTran page={} 拉取成功, count={} vendorId={}",
                    page, resp.getBody() != null ? resp.getBody().size() : 0, vendorId);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("积成 customTran 请求失败 page={} vendorId={}: {}", page, vendorId, e.getMessage());
            return null;
        }
    }
}
