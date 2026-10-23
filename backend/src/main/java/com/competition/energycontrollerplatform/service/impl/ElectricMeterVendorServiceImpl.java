package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.ElectricMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.ElectricMeterVendor;
import com.competition.energycontrollerplatform.mapper.ElectricMeterVendorMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.ElectricMeterVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ElectricMeterVendorServiceImpl implements ElectricMeterVendorService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final ElectricMeterVendorMapper electricMeterVendorMapper;

    @Override
    public IPage<ElectricMeterVendor> listPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<ElectricMeterVendor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectricMeterVendor::getIsDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ElectricMeterVendor::getVendorName, kw)
                    .or().like(ElectricMeterVendor::getApiUrl, kw)
                    .or().like(ElectricMeterVendor::getAppId, kw));
        }
        wrapper.orderByDesc(ElectricMeterVendor::getStatus)
                .orderByDesc(ElectricMeterVendor::getLastEnabledTime)
                .orderByDesc(ElectricMeterVendor::getCreateTime);
        return electricMeterVendorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ElectricMeterVendor getById(Long id) {
        ElectricMeterVendor vendor = electricMeterVendorMapper.selectById(id);
        if (vendor == null) {
            throw BusinessException.of(404, "电表厂商不存在");
        }
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElectricMeterVendor create(ElectricMeterVendorRequest request) {
        Integer status = normalizeStatus(request.getStatus());
        String vendorName = trim(request.getVendorName());

        Long exists = electricMeterVendorMapper.selectCount(
                new LambdaQueryWrapper<ElectricMeterVendor>()
                        .eq(ElectricMeterVendor::getVendorName, vendorName)
                        .eq(ElectricMeterVendor::getIsDeleted, 0));
        if (exists > 0) {
            throw BusinessException.of(400, "电表厂商名称已存在: " + vendorName);
        }

        ElectricMeterVendor vendor = new ElectricMeterVendor();
        vendor.setVendorName(vendorName);
        vendor.setApiUrl(trimTrailingSlash(trim(request.getApiUrl())));
        vendor.setAppId(trim(request.getAppId()));
        vendor.setAppSecret(trim(request.getAppSecret()));
        vendor.setWebhookSignature(trimToNull(request.getWebhookSignature()));
        vendor.setStatus(status);
        vendor.setCreateBy(SecurityUserContext.getCurrentUsername());
        if (status == STATUS_ENABLED) {
            vendor.setLastEnabledTime(LocalDateTime.now());
        }

        electricMeterVendorMapper.insert(vendor);
        if (status == STATUS_ENABLED) {
            electricMeterVendorMapper.disableOthers(vendor.getId());
        }
        return vendor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        int normalizedStatus = normalizeStatus(status);
        ElectricMeterVendor vendor = electricMeterVendorMapper.selectById(id);
        if (vendor == null) {
            throw BusinessException.of(404, "电表厂商不存在");
        }
        vendor.setStatus(normalizedStatus);
        if (normalizedStatus == STATUS_ENABLED) {
            vendor.setLastEnabledTime(LocalDateTime.now());
        }
        electricMeterVendorMapper.updateById(vendor);
        if (normalizedStatus == STATUS_ENABLED) {
            electricMeterVendorMapper.disableOthers(id);
        }
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? STATUS_ENABLED : status;
        if (value != STATUS_DISABLED && value != STATUS_ENABLED) {
            throw BusinessException.of(400, "状态只能为启用或停用");
        }
        return value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
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
