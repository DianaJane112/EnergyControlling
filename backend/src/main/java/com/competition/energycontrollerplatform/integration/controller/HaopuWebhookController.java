package com.competition.energycontrollerplatform.integration.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.integration.config.ElectricMeterVendorConfigProvider;
import com.competition.energycontrollerplatform.integration.dto.HaopuDTO;
import com.competition.energycontrollerplatform.integration.dto.HaopuArchiveSyncResult;
import com.competition.energycontrollerplatform.integration.dto.HaopuReadingSyncResult;
import com.competition.energycontrollerplatform.integration.http.client.HaopuElectricClient;
import com.competition.energycontrollerplatform.integration.service.MeterDataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 昊普电表 Webhook 控制器。
 * <p>
 * 接收昊普平台主动推送的电表采集数据（接口一）。
 * 昊普将数据 POST 到本接口，Body 为 JSON 数组，支持单次上报多条电表读数。
 * <p>
 * <b>安全说明：</b>此接口为机器对机器（M2M）通信，
 * 生产环境应在 Spring Security 中放行此路径，
 * 当前支持基于后台“电表对接”配置的可选 webhook 签名校验。
 * 若配置了签名密钥，则请求必须携带匹配的签名头或参数。
 *
 * @see MeterDataSyncService#syncHaopuReadings(List)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/integration/haopu")
@RequiredArgsConstructor
@Tag(name = "昊普电表集成", description = "接收昊普平台主动推送的电表采集数据")
public class HaopuWebhookController {

    private final MeterDataSyncService meterDataSyncService;
    private final ElectricMeterVendorConfigProvider electricMeterVendorConfigProvider;
    private final HaopuElectricClient haopuElectricClient;

    /**
     * 电表数据采集（主动推送）。
     * <p>
     * 对应昊普文档「接口一」：昊普平台将电表全量运行数据（电压、电流、功率、
     * 功率因数、分时电量等 25 个字段）以 JSON 数组形式 POST 到此端点。
     * 本接口同步处理入库后立即返回 200，阻塞等待持久化完成。
     *
     * @param readings JSON 数组，每项为一条电表采集数据
     * @return success 表示已接收并处理
     */
    @PostMapping("/webhook")
    @Operation(
            summary = "电表数据采集（主动推送）",
            description = "接收昊普 POST 推送的电表全量运行数据（电压/电流/功率/功率因数/分时电量），"
                    + "Body 为 JSON 数组，支持单次上报多条。匹配到平台电表后写入 t_meter_reading。"
    )
    public Result<Map<String, Object>> receiveReadings(@RequestBody List<HaopuDTO.ReadingItem> readings,
                                        HttpServletRequest request) {
        if (!verifyWebhookSignature(request)) {
            return Result.error(Result.CODE_UNAUTHORIZED, "Webhook签名校验失败");
        }
        if (readings == null || readings.isEmpty()) {
            log.warn("昊普webhook: 收到空数据包");
            return Result.success("ok", Map.of("received", 0));
        }
        String requestId = java.util.UUID.randomUUID().toString();
        // Audit identifiable devices before filtering report/timestamp.
        for (int i = 0; i < readings.size(); i++) {
            HaopuDTO.ReadingItem item = readings.get(i);
            if (item != null && item.getCollectorCode() != null && item.getMeterCode() != null) {
                meterDataSyncService.recordHaopuReceive(item);
            }
            if (item == null || !Boolean.TRUE.equals(item.getReport()) || item.getCollectorCode() == null
                    || item.getMeterCode() == null || item.getTimestamp() == null) {
                log.warn("Haopu rejected requestId={} index={} collectorCode={} meterCode={} report={} timestamp={}",
                        requestId, i, item == null ? null : item.getCollectorCode(),
                        item == null ? null : item.getMeterCode(), item == null ? null : item.getReport(),
                        item == null ? null : item.getTimestamp());
            }
        }
        List<HaopuDTO.ReadingItem> validReadings = readings.stream()
                .filter(r -> r != null && Boolean.TRUE.equals(r.getReport())
                        && r.getCollectorCode() != null && r.getMeterCode() != null && r.getTimestamp() != null)
                .toList();
        log.info("昊普webhook: received={}, valid={}, rejected={}", readings.size(), validReadings.size(), readings.size() - validReadings.size());
        logExtraFields(readings);
        Set<Long> collectorCodes = validReadings.stream()
                .map(HaopuDTO.ReadingItem::getCollectorCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long collectorCode : collectorCodes) {
            try {
                HaopuArchiveSyncResult result = meterDataSyncService.syncHaopuArchivesWithResult(
                        collectorCode, haopuElectricClient.fetchMeterArchives(collectorCode));
                meterDataSyncService.recordHaopuArchiveSync(collectorCode, result, null);
            } catch (Exception e) {
                log.error("Haopu archive fetch/sync failed collectorCode={}: {}", collectorCode, e.getMessage());
                meterDataSyncService.recordHaopuArchiveSync(collectorCode,
                        new HaopuArchiveSyncResult(0, 0, 0, 0, 0), abbreviate(e.getMessage()));
            }
        }
        HaopuReadingSyncResult sync = meterDataSyncService.syncHaopuReadings(validReadings);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requestId", requestId);
        summary.put("received", readings.size());
        summary.put("uniqueDevices", readings.stream().filter(Objects::nonNull)
                .filter(r -> r.getCollectorCode() != null && r.getMeterCode() != null)
                .map(r -> r.getCollectorCode() + "|" + r.getMeterCode()).distinct().count());
        summary.put("valid", validReadings.size());
        summary.put("inserted", sync.inserted());
        summary.put("rejected", readings.size() - validReadings.size() + sync.rejected());
        summary.put("failed", sync.failed());
        summary.put("processingStatus", sync.failed() > 0 || sync.rejected() > 0
                || validReadings.size() != readings.size() ? "PARTIAL_FAILURE" : "COMPLETED");
        log.info("Haopu webhook result {}", summary);
        // Preserve the established vendor acknowledgment until its retry protocol is confirmed.
        // processingStatus describes ingestion; transport acknowledgment alone is not persistence proof.
        return Result.success("ok", summary);
    }

    private void logExtraFields(List<HaopuDTO.ReadingItem> readings) {
        Set<String> fieldNames = new TreeSet<>();
        int itemsWithExtras = 0;
        int loggedSamples = 0;
        for (HaopuDTO.ReadingItem item : readings) {
            if (item == null || item.getExtraFields().isEmpty()) continue;
            itemsWithExtras++;
            fieldNames.addAll(item.getExtraFields().keySet());
            if (loggedSamples++ < 20) {
                log.warn("Haopu undocumented fields collectorCode={} meterCode={} fields={}",
                        item.getCollectorCode(), item.getMeterCode(), sanitizeExtraFields(item.getExtraFields()));
            }
        }
        log.info("Haopu field audit: itemsWithExtras={} extraFieldNames={}", itemsWithExtras, fieldNames);
    }

    private Map<String, String> sanitizeExtraFields(Map<String, Object> fields) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        fields.forEach((name, value) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            boolean sensitive = lower.contains("token") || lower.contains("secret") || lower.contains("password")
                    || lower.contains("signature") || lower.equals("sign") || lower.contains("authorization");
            String text = sensitive ? "***" : String.valueOf(value);
            sanitized.put(name, text.length() > 200 ? text.substring(0, 200) + "..." : text);
        });
        return sanitized;
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) return value;
        return value.substring(0, 1000);
    }

    private boolean verifyWebhookSignature(HttpServletRequest request) {
        ElectricMeterVendorConfigProvider.ElectricVendorCredentials credentials =
                electricMeterVendorConfigProvider.resolveCredentials();
        String expected = credentials.webhookSignature();
        if (!StringUtils.hasText(expected)) {
            log.debug("昊普webhook: 未配置 webhook 签名，跳过验签");
            return true;
        }

        String provided = resolveIncomingSignature(request);
        if (!StringUtils.hasText(provided)) {
            log.warn("昊普webhook: 缺少签名头或签名参数");
            return false;
        }

        boolean matched = MessageDigest.isEqual(
                expected.trim().getBytes(StandardCharsets.UTF_8),
                provided.trim().getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            log.warn("昊普webhook: 签名不匹配");
        }
        return matched;
    }

    private String resolveIncomingSignature(HttpServletRequest request) {
        String[] headerNames = {
                "X-Haopu-Signature",
                "X-Webhook-Signature",
                "X-Signature",
                "Signature",
        };
        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        String[] parameterNames = {"sign", "signature", "webhookSign"};
        for (String paramName : parameterNames) {
            String value = request.getParameter(paramName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
