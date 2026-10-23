package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.WaterMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.WaterMeterVendor;
import com.competition.energycontrollerplatform.mapper.WaterMeterVendorMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.WaterMeterVendorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WaterMeterVendorServiceImpl implements WaterMeterVendorService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final WaterMeterVendorMapper waterMeterVendorMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<WaterMeterVendor> listPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<WaterMeterVendor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterMeterVendor::getIsDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(WaterMeterVendor::getVendorName, kw)
                    .or().like(WaterMeterVendor::getVendorCode, kw)
                    .or().like(WaterMeterVendor::getProtocolType, kw)
                    .or().like(WaterMeterVendor::getApiUrl, kw)
                    .or().like(WaterMeterVendor::getAppId, kw));
        }
        wrapper.orderByDesc(WaterMeterVendor::getStatus)
                .orderByDesc(WaterMeterVendor::getLastEnabledTime)
                .orderByDesc(WaterMeterVendor::getCreateTime);
        return waterMeterVendorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public WaterMeterVendor getById(Long id) {
        WaterMeterVendor vendor = waterMeterVendorMapper.selectById(id);
        if (vendor == null || vendor.getIsDeleted() != null && vendor.getIsDeleted() == 1) {
            throw BusinessException.of(404, "水表厂商不存在");
        }
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterMeterVendor create(WaterMeterVendorRequest request) {
        Integer status = normalizeStatus(request.getStatus());
        String vendorName = trimRequired(request.getVendorName(), "水表厂商名称不能为空");
        String vendorCode = normalizeCode(request.getVendorCode(), "厂商编码不能为空");
        String protocolType = normalizeCode(request.getProtocolType(), "协议类型不能为空");
        String configJson = normalizeConfigJson(request.getConfigJson());

        Long exists = waterMeterVendorMapper.selectCount(
                new LambdaQueryWrapper<WaterMeterVendor>()
                        .eq(WaterMeterVendor::getVendorCode, vendorCode)
                        .eq(WaterMeterVendor::getVendorName, vendorName)
                        .eq(WaterMeterVendor::getIsDeleted, 0));
        if (exists > 0) {
            throw BusinessException.of(400, "同厂商编码下水表厂商名称已存在: " + vendorName);
        }

        WaterMeterVendor vendor = new WaterMeterVendor();
        vendor.setVendorName(vendorName);
        vendor.setVendorCode(vendorCode);
        vendor.setProtocolType(protocolType);
        vendor.setApiUrl(trimTrailingSlash(trimToNull(request.getApiUrl())));
        vendor.setAuthType(normalizeOptionalCode(request.getAuthType()));
        vendor.setAppId(trimToNull(request.getAppId()));
        vendor.setAppSecret(trimToNull(request.getAppSecret()));
        vendor.setWebhookSignature(trimToNull(request.getWebhookSignature()));
        vendor.setConfigJson(configJson);
        vendor.setStatus(status);
        vendor.setCreateBy(SecurityUserContext.getCurrentUsername());
        if (status == STATUS_ENABLED) {
            vendor.setLastEnabledTime(LocalDateTime.now());
        }

        waterMeterVendorMapper.insert(vendor);
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        int normalizedStatus = normalizeStatus(status);
        WaterMeterVendor vendor = waterMeterVendorMapper.selectById(id);
        if (vendor == null || vendor.getIsDeleted() != null && vendor.getIsDeleted() == 1) {
            throw BusinessException.of(404, "水表厂商不存在");
        }
        vendor.setStatus(normalizedStatus);
        if (normalizedStatus == STATUS_ENABLED) {
            vendor.setLastEnabledTime(LocalDateTime.now());
        }
        waterMeterVendorMapper.updateById(vendor);
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? STATUS_ENABLED : status;
        if (value != STATUS_DISABLED && value != STATUS_ENABLED) {
            throw BusinessException.of(400, "状态只能是启用或停用");
        }
        return value;
    }

    private String normalizeConfigJson(String configJson) {
        String trimmed = trimToNull(configJson);
        if (trimmed == null) {
            return null;
        }
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception e) {
            throw BusinessException.of(400, "厂商扩展配置必须是合法JSON");
        }
    }

    private String normalizeCode(String value, String message) {
        String trimmed = trimRequired(value, message);
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw BusinessException.of(400, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
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
}
