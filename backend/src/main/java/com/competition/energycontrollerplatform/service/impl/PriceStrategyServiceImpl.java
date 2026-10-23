package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.dto.PriceStrategyRequest;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import com.competition.energycontrollerplatform.mapper.PriceStrategyMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.PriceStrategyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PriceStrategyServiceImpl implements PriceStrategyService {

    private final PriceStrategyMapper priceStrategyMapper;
    private final CacheNullValueHelper cacheNullValueHelper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<PriceStrategy> listPage(int pageNum, int pageSize, String keyword, Integer meterType) {
        LambdaQueryWrapper<PriceStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceStrategy::getIsDeleted, 0);
        if (meterType != null) {
            validateMeterType(meterType);
            wrapper.eq(PriceStrategy::getMeterType, meterType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PriceStrategy::getStrategyName, keyword)
                    .or().like(PriceStrategy::getStrategyCode, keyword));
        }
        wrapper.orderByDesc(PriceStrategy::getCreateTime);
        Page<PriceStrategy> page = new Page<>(pageNum, pageSize);
        return priceStrategyMapper.selectPage(page, wrapper);
    }

    @Override
    @Cacheable(value = "price_strategy", key = "#id")
    public PriceStrategy getById(Long id) {
        if (cacheNullValueHelper.isNullCached("price_strategy", id)) {
            throw BusinessException.of(404, "费率策略不存在");
        }
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            cacheNullValueHelper.cacheNull("price_strategy", id);
            throw BusinessException.of(404, "费率策略不存在");
        }
        return strategy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PriceStrategy create(PriceStrategyRequest request) {
        PriceStrategy strategy = new PriceStrategy();
        applyRequest(strategy, request);
        String groupCode = StringUtils.hasText(request.getVersionGroupCode())
                ? request.getVersionGroupCode().trim() : request.getStrategyCode().trim();
        validatePeriod(request);
        if (priceStrategyMapper.countOverlapping(groupCode, request.getMeterType(),
                request.getEffectiveDate(), request.getExpireDate()) > 0) {
            throw BusinessException.of(400, "同一费率版本组的生效区间不能重叠");
        }
        strategy.setVersionGroupCode(groupCode);
        strategy.setVersionNo(priceStrategyMapper.maxVersionNo(groupCode, request.getMeterType()) + 1);
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        strategy.setCreateBy(SecurityUserContext.getCurrentUsername());
        priceStrategyMapper.insert(strategy);
        return strategy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "price_strategy", key = "#id")
    public PriceStrategy update(Long id, PriceStrategyRequest request) {
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            throw BusinessException.of("费率策略不存在");
        }
        if (!strategy.getMeterType().equals(request.getMeterType())) {
            throw BusinessException.of(400, "费率策略所属表计类型不可修改，请新建对应类型的策略");
        }
        if (strategy.getEffectiveDate() != null && !strategy.getEffectiveDate().isAfter(java.time.LocalDate.now())) {
            throw BusinessException.of(400, "已生效费率不可覆盖修改，请创建新版本");
        }
        validatePeriod(request);
        String groupCode = StringUtils.hasText(request.getVersionGroupCode())
                ? request.getVersionGroupCode().trim() : strategy.getVersionGroupCode();
        if (priceStrategyMapper.countOverlappingExcluding(id, groupCode, request.getMeterType(),
                request.getEffectiveDate(), request.getExpireDate()) > 0) {
            throw BusinessException.of(400, "鍚屼竴璐圭巼鐗堟湰缁勭殑鐢熸晥鍖洪棿涓嶈兘閲嶅彔");
        }
        applyRequest(strategy, request);
        strategy.setVersionGroupCode(groupCode);
        strategy.setUpdateTime(LocalDateTime.now());
        priceStrategyMapper.updateById(strategy);
        return strategy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "price_strategy", key = "#id")
    public void delete(Long id) {
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            throw BusinessException.of("费率策略不存在");
        }
        priceStrategyMapper.logicDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "price_strategy", key = "#id")
    public void updateStatus(Long id, Integer status) {
        PriceStrategy strategy = priceStrategyMapper.selectById(id);
        if (strategy == null) {
            throw BusinessException.of("费率策略不存在");
        }
        strategy.setStatus(status);
        strategy.setUpdateTime(LocalDateTime.now());
        priceStrategyMapper.updateById(strategy);
    }

    private void applyRequest(PriceStrategy strategy, PriceStrategyRequest req) {
        validateMeterType(req.getMeterType());
        validatePriceType(req.getPriceType());
        validateTimeSlots(req.getPriceType(), req.getTimeSlots());
        strategy.setStrategyName(req.getStrategyName());
        strategy.setStrategyCode(req.getStrategyCode());
        if (StringUtils.hasText(req.getVersionGroupCode())) strategy.setVersionGroupCode(req.getVersionGroupCode().trim());
        strategy.setProjectId(req.getProjectId());
        strategy.setMeterType(req.getMeterType());
        strategy.setPriceType(req.getPriceType());
        strategy.setTimeSlots(req.getTimeSlots());
        strategy.setFixedPrice(req.getFixedPrice());
        strategy.setPlusPrice(req.getPlusPrice());
        strategy.setEffectiveDate(req.getEffectiveDate());
        strategy.setExpireDate(req.getExpireDate());
        strategy.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        strategy.setRemark(req.getRemark());
    }

    private void validatePeriod(PriceStrategyRequest req) {
        if (req.getExpireDate() != null && req.getExpireDate().isBefore(req.getEffectiveDate())) {
            throw BusinessException.of(400, "失效日期不能早于生效日期");
        }
    }

    private void validatePriceType(Integer priceType) {
        if (priceType == null || (priceType < 1 || priceType > 3)) {
            throw BusinessException.of(400, "策略类型只能是1固定、2分时或3阶梯");
        }
    }

    /** 校验分时策略为整点、互斥且完整覆盖当天24小时。 */
    private void validateTimeSlots(Integer priceType, String json) {
        if (!Integer.valueOf(2).equals(priceType)) return;
        if (json == null || json.isBlank()) throw BusinessException.of(400, "分时策略必须配置完整的尖峰平谷时段");
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray() || arr.isEmpty()) throw new IllegalArgumentException();
            boolean[] covered = new boolean[24];
            Set<String> names = Set.of("尖", "峰", "平", "谷", "SHARP", "PEAK", "FLAT", "VALLEY");
            for (JsonNode s : arr) {
                String name = s.path("slotName").asText(s.path("type").asText(""));
                if (!names.contains(name)) throw new IllegalArgumentException("费率类型非法");
                if (!s.has("startHour") || !s.has("endHour") || !s.has("price")) throw new IllegalArgumentException("时段字段不完整");
                int start = s.get("startHour").asInt(-1), end = s.get("endHour").asInt(-1);
                if (start < 0 || start > 23 || end < 0 || end > 24 || start == end) throw new IllegalArgumentException("时段范围非法");
                if (!s.get("startHour").canConvertToInt() || !s.get("endHour").canConvertToInt()) throw new IllegalArgumentException("时段必须为整点");
                if (!s.get("price").isNumber() || s.get("price").decimalValue().signum() < 0) throw new IllegalArgumentException("价格必须为非负数字");
                // end=24 表示午夜（当天结束），按 start..23 覆盖；其余（含跨午夜 end<start）按取模循环覆盖
                if (end == 24) {
                    for (int h = start; h < 24; h++) {
                        if (covered[h]) throw new IllegalArgumentException("时段存在重叠");
                        covered[h] = true;
                    }
                } else {
                    for (int h = start; h != end; h = (h + 1) % 24) {
                        if (covered[h]) throw new IllegalArgumentException("时段存在重叠");
                        covered[h] = true;
                    }
                }
            }
            for (boolean v : covered) if (!v) throw new IllegalArgumentException("分时策略必须完整覆盖00:00-24:00");
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw BusinessException.of(400, e.getMessage() == null ? "分时时段格式非法" : e.getMessage()); }
    }

    private void validateMeterType(Integer meterType) {
        if (meterType == null || (meterType != 0 && meterType != 1)) {
            throw BusinessException.of(400, "表计类型只能是水表(0)或电表(1)");
        }
    }
}
