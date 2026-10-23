package com.competition.energycontrollerplatform.integration.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.WaterMeterVendor;
import com.competition.energycontrollerplatform.mapper.WaterMeterVendorMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime water meter vendor configuration resolver.
 */
@Component
@RequiredArgsConstructor
public class WaterMeterVendorConfigProvider {

    public static final String PROTOCOL_JICHENG_HTTP = "JICHENG_HTTP";
    public static final String PROTOCOL_JIEXIAN_HTTP = "JIEXIAN_HTTP";

    private static final Logger log = LoggerFactory.getLogger(WaterMeterVendorConfigProvider.class);

    private final WaterMeterVendorMapper waterMeterVendorMapper;
    private final VendorApiProperties vendorApiProperties;
    private final ObjectMapper objectMapper;

    public List<Long> listEnabledVendorIdsByProtocol(String protocolType) {
        try {
            return waterMeterVendorMapper.selectList(
                            new LambdaQueryWrapper<WaterMeterVendor>()
                                    .select(WaterMeterVendor::getId)
                                    .eq(WaterMeterVendor::getProtocolType, protocolType)
                                    .eq(WaterMeterVendor::getStatus, 1)
                                    .eq(WaterMeterVendor::getIsDeleted, 0)
                                    .orderByAsc(WaterMeterVendor::getId))
                    .stream()
                    .map(WaterMeterVendor::getId)
                    .toList();
        } catch (RuntimeException e) {
            log.debug("未读取到水表厂商数据库配置: {}", e.getMessage());
            return List.of();
        }
    }

    public JichengVendorCredentials resolveJichengCredentials() {
        return resolveJichengCredentials(null);
    }

    public JichengVendorCredentials resolveJichengCredentials(Long vendorId) {
        WaterMeterVendor vendor = vendorId != null
                ? findEnabledVendorById(vendorId)
                : findActiveVendor(PROTOCOL_JICHENG_HTTP);
        if (vendor != null) {
            if (!PROTOCOL_JICHENG_HTTP.equals(vendor.getProtocolType())) {
                return JichengVendorCredentials.empty(vendor.getVendorName());
            }
            WaterVendorCredentials credentials = buildCredentials(vendor);
            return new JichengVendorCredentials(
                    credentials.vendorName(),
                    credentials.baseUrl(),
                    firstText(credentials.configValue("sign"), credentials.appSecret()),
                    true);
        }

        if (vendorId != null) {
            return JichengVendorCredentials.empty("water_vendor_id=" + vendorId);
        }

        VendorApiProperties.Jicheng fallback = vendorApiProperties.getJicheng();
        return new JichengVendorCredentials(
                "application.yml",
                trimTrailingSlash(fallback.getBaseUrl()),
                fallback.getSign(),
                false);
    }

    public JiexianVendorCredentials resolveJiexianCredentials() {
        return resolveJiexianCredentials(null);
    }

    public JiexianVendorCredentials resolveJiexianCredentials(Long vendorId) {
        WaterMeterVendor vendor = vendorId != null
                ? findEnabledVendorById(vendorId)
                : findActiveVendor(PROTOCOL_JIEXIAN_HTTP);
        if (vendor != null) {
            if (!PROTOCOL_JIEXIAN_HTTP.equals(vendor.getProtocolType())) {
                return JiexianVendorCredentials.empty(vendor.getVendorName());
            }
            WaterVendorCredentials credentials = buildCredentials(vendor);
            return new JiexianVendorCredentials(
                    credentials.vendorName(),
                    credentials.baseUrl(),
                    firstText(
                            credentials.configValue("companyCode"),
                            credentials.configValue("company_code"),
                            credentials.appId()),
                    true);
        }

        if (vendorId != null) {
            return JiexianVendorCredentials.empty("water_vendor_id=" + vendorId);
        }

        VendorApiProperties.Jiexian fallback = vendorApiProperties.getJiexian();
        return new JiexianVendorCredentials(
                "application.yml",
                trimTrailingSlash(fallback.getBaseUrl()),
                fallback.getCompanyCode(),
                false);
    }

    private WaterMeterVendor findEnabledVendorById(Long vendorId) {
        try {
            WaterMeterVendor vendor = waterMeterVendorMapper.selectById(vendorId);
            if (vendor == null
                    || vendor.getStatus() == null
                    || vendor.getStatus() != 1
                    || vendor.getIsDeleted() != null && vendor.getIsDeleted() == 1) {
                return null;
            }
            return vendor;
        } catch (RuntimeException e) {
            log.debug("读取水表厂商配置失败 id={}: {}", vendorId, e.getMessage());
            return null;
        }
    }

    private WaterMeterVendor findActiveVendor(String protocolType) {
        try {
            return waterMeterVendorMapper.selectOne(
                    new LambdaQueryWrapper<WaterMeterVendor>()
                            .eq(WaterMeterVendor::getProtocolType, protocolType)
                            .eq(WaterMeterVendor::getStatus, 1)
                            .eq(WaterMeterVendor::getIsDeleted, 0)
                            .orderByDesc(WaterMeterVendor::getLastEnabledTime)
                            .orderByDesc(WaterMeterVendor::getUpdateTime)
                            .last("LIMIT 1"));
        } catch (RuntimeException e) {
            log.debug("未读取到水表厂商数据库配置，使用 application.yml 默认配置: {}", e.getMessage());
            return null;
        }
    }

    private WaterVendorCredentials buildCredentials(WaterMeterVendor vendor) {
        return new WaterVendorCredentials(
                vendor.getVendorName(),
                vendor.getVendorCode(),
                vendor.getProtocolType(),
                trimTrailingSlash(vendor.getApiUrl()),
                vendor.getAuthType(),
                vendor.getAppId(),
                vendor.getAppSecret(),
                vendor.getWebhookSignature(),
                parseConfig(vendor.getConfigJson()),
                true);
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("水表厂商扩展配置JSON解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public record WaterVendorCredentials(
            String vendorName,
            String vendorCode,
            String protocolType,
            String baseUrl,
            String authType,
            String appId,
            String appSecret,
            String webhookSignature,
            Map<String, Object> config,
            boolean fromDatabase) {

        public String configValue(String key) {
            Object value = config == null ? null : config.get(key);
            return value == null ? null : Objects.toString(value, null);
        }
    }

    public record JichengVendorCredentials(
            String vendorName,
            String baseUrl,
            String sign,
            boolean fromDatabase) {

        public static JichengVendorCredentials empty(String vendorName) {
            return new JichengVendorCredentials(vendorName, null, null, true);
        }
    }

    public record JiexianVendorCredentials(
            String vendorName,
            String baseUrl,
            String companyCode,
            boolean fromDatabase) {

        public static JiexianVendorCredentials empty(String vendorName) {
            return new JiexianVendorCredentials(vendorName, null, null, true);
        }
    }
}
