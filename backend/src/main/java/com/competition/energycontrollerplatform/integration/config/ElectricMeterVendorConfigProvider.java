package com.competition.energycontrollerplatform.integration.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.ElectricMeterVendor;
import com.competition.energycontrollerplatform.mapper.ElectricMeterVendorMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 电表厂商运行时配置提供器。
 * <p>优先使用后台“电表对接”启用的配置，未配置或表结构未初始化时回退到 application.yml。</p>
 */
@Component
@RequiredArgsConstructor
public class ElectricMeterVendorConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(ElectricMeterVendorConfigProvider.class);

    private final ElectricMeterVendorMapper electricMeterVendorMapper;
    private final VendorApiProperties vendorApiProperties;

    public ElectricVendorCredentials resolveCredentials() {
        ElectricMeterVendor dbVendor = findActiveVendor();
        if (dbVendor != null
                && StringUtils.hasText(dbVendor.getApiUrl())
                && StringUtils.hasText(dbVendor.getAppId())
                && StringUtils.hasText(dbVendor.getAppSecret())) {
            String webhookSignature = normalizeWebhookSignature(dbVendor.getWebhookSignature());
            if (!StringUtils.hasText(webhookSignature)) {
                webhookSignature = normalizeWebhookSignature(vendorApiProperties.getHaopu().getWebhookSignature());
            }
            return new ElectricVendorCredentials(
                    dbVendor.getVendorName(),
                    trimTrailingSlash(dbVendor.getApiUrl()),
                    dbVendor.getAppId(),
                    dbVendor.getAppSecret(),
                    webhookSignature,
                    true);
        }

        VendorApiProperties.Haopu fallback = vendorApiProperties.getHaopu();
        return new ElectricVendorCredentials(
                "application.yml",
                trimTrailingSlash(fallback.getBaseUrl()),
                fallback.getAppId(),
                fallback.getAppSecret(),
                normalizeWebhookSignature(fallback.getWebhookSignature()),
                false);
    }

    private ElectricMeterVendor findActiveVendor() {
        try {
            return electricMeterVendorMapper.selectOne(
                    new LambdaQueryWrapper<ElectricMeterVendor>()
                            .eq(ElectricMeterVendor::getStatus, 1)
                            .eq(ElectricMeterVendor::getIsDeleted, 0)
                            .orderByDesc(ElectricMeterVendor::getLastEnabledTime)
                            .orderByDesc(ElectricMeterVendor::getUpdateTime)
                            .last("LIMIT 1"));
        } catch (RuntimeException e) {
            log.debug("未读取到数据库电表厂商配置，使用 application.yml 默认配置: {}", e.getMessage());
            return null;
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

    private String normalizeWebhookSignature(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ElectricVendorCredentials(
            String vendorName,
            String baseUrl,
            String appId,
            String appSecret,
            String webhookSignature,
            boolean fromDatabase) {
    }
}
