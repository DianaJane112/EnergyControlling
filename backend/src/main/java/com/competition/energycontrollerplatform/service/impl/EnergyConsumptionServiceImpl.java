package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.service.EnergyConsumptionService;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 能耗消费计算与自动扣费实现。
 * <p>
 * 核心流程：
 * <ol>
 *   <li>取表计最新读数</li>
 *   <li>取上次结算读数（无则用 startVal 初始读数）</li>
 *   <li>差值 × 费率 → 应扣金额</li>
 *   <li>原子扣减商户余额（AND balance >= cost）</li>
 *   <li>写入资金流水（tradeType=2）</li>
 *   <li>更新表计的 lastSettledReading</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class EnergyConsumptionServiceImpl implements EnergyConsumptionService {

    private static final Logger log = LoggerFactory.getLogger(EnergyConsumptionServiceImpl.class);

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final CustomerMapper customerMapper;
    private final PriceStrategyMapper priceStrategyMapper;
    private final ShadowMeterMapper shadowMeterMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final MeterOperationTraceService meterOperationTraceService;
    private final ObjectMapper objectMapper;
    private final BillingSegmentMapper billingSegmentMapper;
    private final MeterGroupMapper meterGroupMapper;
    private final ProjectMapper projectMapper;

    @Value("${billing.default-unit-price:0.85}")
    private BigDecimal configDefaultPrice;

    // ──────────────────────── 主流程 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal calculateAndDebit(Long meterId, BigDecimal defaultPrice) {
        // ① 加载表计信息
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.warn("[自动扣费] 表计不存在 meterId={}", meterId);
            return BigDecimal.ZERO;
        }
        BillingTarget billingTarget = resolveBillingTarget(meter);
        if (billingTarget.customerId() == null) {
            log.warn("[自动扣费] 表计未绑定商户 meterId={}", meterId);
            return BigDecimal.ZERO;
        }

        // ② 取最新读数
        MeterReading latestReading = meterReadingMapper.getLatestByMeterId(meterId);
        if (latestReading == null || latestReading.getReadVal() == null) {
            log.debug("[自动扣费] 表计无读数 meterId={}", meterId);
            return BigDecimal.ZERO;
        }
        BigDecimal currentReading = latestReading.getReadVal();

        // ③ 确定上次结算读数（首次用 startVal）
        BigDecimal lastReading;
        if (meter.getLastSettledReading() != null) {
            lastReading = meter.getLastSettledReading();
        } else {
            // 首次结算，以表计建档时的初始读数为基准
            lastReading = parseStartVal(meter.getStartVal());
        }

        // ④ 按真实读数区间拆段；每段按结束时间选择当时有效的费率版本。
        MeterReading previousReading = meter.getLastSettledTime() == null ? null
                : meterReadingMapper.getLatestBeforeTime(meterId, meter.getLastSettledTime());
        List<MeterReading> settlementReadings = meter.getLastSettledTime() == null
                || latestReading.getTimestamp() == null ? List.of(latestReading)
                : meterReadingMapper.listForSettlement(meterId, meter.getLastSettledTime(), latestReading.getTimestamp());
        if (settlementReadings.isEmpty()) settlementReadings = List.of(latestReading);

        BigDecimal ratio = defaultRatio(meter.getCtRatio()).multiply(defaultRatio(meter.getPtRatio()));
        List<SegmentComputation> segments = new ArrayList<>();
        Map<YearMonth, BigDecimal> tierUsageByMonth = new HashMap<>();
        BigDecimal segmentPreviousValue = lastReading;
        MeterReading segmentPreviousReading = previousReading;
        LocalDateTime segmentStart = meter.getLastSettledTime();
        for (MeterReading reading : settlementReadings) {
            if (reading.getReadVal() == null) continue;
            BigDecimal segmentRaw = reading.getReadVal().subtract(segmentPreviousValue);
            if (segmentRaw.signum() > 0) {
                BigDecimal segmentConsumption = segmentRaw.multiply(ratio);
                LocalDateTime pricingTime = reading.getTimestamp() == null ? LocalDateTime.now() : reading.getTimestamp();
                YearMonth pricingMonth = YearMonth.from(pricingTime);
                BigDecimal consumptionBeforePeriod = tierUsageByMonth.computeIfAbsent(pricingMonth, month -> {
                    BigDecimal persisted = billingSegmentMapper.sumPeriodConsumption(
                            billingTarget.accountMeter().getId(), month.atDay(1).atStartOfDay(), pricingTime);
                    return persisted == null ? BigDecimal.ZERO : persisted;
                });
                BillingPrice price = resolveBillingPrice(billingTarget.accountMeter(), meter,
                        reading, segmentPreviousReading, segmentConsumption, ratio, defaultPrice,
                        consumptionBeforePeriod);
                segments.add(new SegmentComputation(segmentStart, reading.getTimestamp(), segmentPreviousValue,
                        reading.getReadVal(), segmentRaw, segmentConsumption, price));
                tierUsageByMonth.put(pricingMonth, consumptionBeforePeriod.add(segmentConsumption));
            }
            segmentPreviousValue = reading.getReadVal();
            segmentPreviousReading = reading;
            segmentStart = reading.getTimestamp();
        }
        if (segments.isEmpty()) {
            log.debug("[自动扣费] 读数无增长 meterId={}, current={}, last={}",
                    meterId, currentReading, lastReading);
            return BigDecimal.ZERO;
        }
        BigDecimal rawDiff = segments.stream().map(SegmentComputation::rawConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumption = segments.stream().map(SegmentComputation::billedConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unroundedCost = segments.stream().map(s -> s.price().cost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = unroundedCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitPrice = consumption.signum() == 0 ? BigDecimal.ZERO
                : unroundedCost.divide(consumption, 8, RoundingMode.HALF_UP);
        BillingPrice lastBillingPrice = segments.get(segments.size() - 1).price();
        boolean multipleVersions = segments.stream().map(s -> s.price().rateConfigId()).distinct().count() > 1;
        BillingPrice billingPrice = new BillingPrice(unitPrice.subtract(lastBillingPrice.plusPrice()),
                lastBillingPrice.plusPrice(), unitPrice, unroundedCost, lastBillingPrice.rateConfigId(),
                lastBillingPrice.priceType(), multipleVersions ? "MULTI_VERSION" : lastBillingPrice.billingType(),
                Map.of("segments", segments.stream().map(this::segmentDetail).toList()));

        log.info("[自动扣费] meterId={}, diff={}, unitPrice={}, cost={}, customerId={}",
                meterId, consumption, unitPrice, cost, billingTarget.customerId());

        // ⑦ 原子扣款
        int rows = customerMapper.subtractBalance(billingTarget.customerId(), cost);
        if (rows == 0) {
            Customer customer = customerMapper.selectById(billingTarget.customerId());
            if (customer == null) {
                log.error("[自动扣费] 商户不存在 customerId={}", billingTarget.customerId());
                return BigDecimal.ZERO;
            }
            // 余额不足 —— 抛出异常触发回滚 + 上层调度器捕获后标记欠费
            throw WalletException.insufficientBalance(
                    billingTarget.customerId(), customer.getBalance(), cost);
        }

        // ⑧ 查最新余额
        Customer customer = customerMapper.selectById(billingTarget.customerId());
        BigDecimal balanceAfter = customer.getBalance();
        BigDecimal balanceBefore = balanceAfter.add(cost);

        // ⑨ 写资金流水（tradeType=2 消费扣费）
        String flowNo = generateFlowNo();
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(flowNo);
        flow.setCustomerId(billingTarget.customerId());
        flow.setMeterId(billingTarget.accountMeter().getId());
        flow.setBalanceBefore(balanceBefore);
        flow.setChangeAmount(cost.negate());
        flow.setBalanceAfter(balanceAfter);
        flow.setTradeType(2); // 2-消费扣费
        flow.setBizNo(flowNo);
        flow.setOperatorName("AUTO_BILLING");
        flow.setOperateTime(LocalDateTime.now());
        flow.setRemark(String.format("自动扣费：读数 %.4f → %.4f，差值 %.4f，单价 %.4f",
                lastReading, currentReading, consumption, unitPrice));
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        accountFlowMapper.insert(flow);
        BillingDimensions dimensions = resolveDimensions(meter);
        for (SegmentComputation segment : segments) {
            persistBillingSegment(flowNo, billingTarget, meter, ratio, dimensions, segment);
        }
        MeterOperationTrace trace = new MeterOperationTrace();
        boolean virtualBilling = !billingTarget.accountMeter().getId().equals(meterId);
        trace.setObjectType(virtualBilling ? "VIRTUAL_METER"
                : (meter.getMeterType() != null && meter.getMeterType() == 1 ? "REAL_ELECTRIC_METER" : "REAL_WATER_METER"));
        trace.setObjectId(billingTarget.accountMeter().getId());
        trace.setMeterId(billingTarget.accountMeter().getId());
        trace.setChildMeterId(virtualBilling ? meterId : null);
        trace.setCustomerId(billingTarget.customerId());
        trace.setEventType("BILLING_SUCCESS"); trace.setOperationTerminal("SYSTEM"); trace.setOperatorName("系统自动");
        trace.setBalanceBefore(balanceBefore); trace.setChangeAmount(cost.negate()); trace.setBalanceAfter(balanceAfter);
        trace.setPreviousReading(lastReading); trace.setRawMeasurement(currentReading); trace.setConsumption(consumption); trace.setBilledMeasurement(consumption);
        trace.setCtRatio(defaultRatio(meter.getCtRatio())); trace.setPtRatio(defaultRatio(meter.getPtRatio()));
        trace.setUnitPrice(billingPrice.basePrice()); trace.setPlusPrice(billingPrice.plusPrice());
        trace.setRateConfigId(billingPrice.rateConfigId()); trace.setPriceType(billingPrice.priceType());
        trace.setBillingType(billingPrice.billingType());
        trace.setPaymentMethod(Integer.valueOf(2).equals(billingTarget.accountMeter().getFeetype()) ? "POSTPAID" : "PREPAID");
        trace.setDetailJson(buildTraceDetail(lastReading, currentReading, rawDiff, consumption,
                meterId, billingTarget.accountMeter().getId(), billingPrice));
        trace.setRequestId(flowNo); trace.setRemark("自动扣费");
        meterOperationTraceService.record(trace);

        // ⑩ 更新表计结算追踪
        meterMapper.updateLastSettledReading(meterId, currentReading, LocalDateTime.now());

        log.info("[自动扣费] 成功 meterId={}, diff={}, cost={}, balance {} → {}",
                meterId, consumption, cost, balanceBefore, balanceAfter);
        return cost;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /** 真实表可挂到虚拟结算账户；余额、费率和资金流水均归属该虚拟账户。 */
    private BillingTarget resolveBillingTarget(Meter sourceMeter) {
        List<ShadowMeter> parents = shadowMeterMapper.findActiveByChildId(sourceMeter.getId()).stream()
                .filter(r -> "REAL_METER".equalsIgnoreCase(r.getShadowType()) && r.getParentMeterId() != null)
                .toList();
        if (parents.size() > 1) {
            throw new IllegalStateException("真实表同时关联多个有效虚拟结算账户，meterId=" + sourceMeter.getId());
        }
        if (parents.size() == 1) {
            Meter virtualMeter = meterMapper.selectById(parents.get(0).getParentMeterId());
            if (virtualMeter != null && "VIRTUAL".equalsIgnoreCase(virtualMeter.getMeterKind())) {
                return new BillingTarget(virtualMeter, virtualMeter.getCustomerId());
            }
        }
        return new BillingTarget(sourceMeter, sourceMeter.getCustomerId());
    }

    private BillingPrice resolveBillingPrice(Meter accountMeter, Meter sourceMeter,
                                             MeterReading current, MeterReading previous,
                                             BigDecimal consumption, BigDecimal ratio,
                                             BigDecimal fallbackPrice,
                                             BigDecimal consumptionBeforePeriod) {
        Long strategyId = accountMeter.getRateConfigId() != null
                ? accountMeter.getRateConfigId() : sourceMeter.getRateConfigId();
        if (strategyId == null) {
            throw new IllegalStateException("结算账户未绑定费率策略，accountMeterId=" + accountMeter.getId());
        }
        PriceStrategy strategy = priceStrategyMapper.selectById(strategyId);
        LocalDate billingDate = current.getTimestamp() != null
                ? current.getTimestamp().toLocalDate() : LocalDate.now();
        if (strategy == null) {
            throw new IllegalStateException("绑定的费率策略不存在，strategyId=" + strategyId);
        }
        String versionGroup = strategy.getVersionGroupCode();
        if (versionGroup != null && !versionGroup.isBlank()) {
            PriceStrategy effective = priceStrategyMapper.findEffectiveVersion(versionGroup,
                    sourceMeter.getMeterType(), billingDate);
            if (effective != null) strategy = effective;
        }
        if (!sourceMeter.getMeterType().equals(strategy.getMeterType())) {
            throw new IllegalStateException("费率策略与表计类型不一致，strategyId=" + strategyId);
        }
        if (!Integer.valueOf(1).equals(strategy.getStatus())
                || strategy.getEffectiveDate() != null && strategy.getEffectiveDate().isAfter(billingDate)
                || strategy.getExpireDate() != null && strategy.getExpireDate().isBefore(billingDate)) {
            throw new IllegalStateException("费率策略在结算日期未生效，strategyId=" + strategyId + ", billingDate=" + billingDate);
        }

        BigDecimal plus = strategy.getPlusPrice() != null ? strategy.getPlusPrice() : BigDecimal.ZERO;
        if (Integer.valueOf(1).equals(strategy.getPriceType())) {
            if (strategy.getFixedPrice() == null || strategy.getFixedPrice().signum() < 0) {
                throw new IllegalStateException("固定费率策略缺少有效单价，strategyId=" + strategy.getId());
            }
            return fixedPrice(strategy.getFixedPrice(), plus, consumption,
                    strategy.getId(), strategy.getPriceType(), "FIXED");
        }
        if (Integer.valueOf(2).equals(strategy.getPriceType())) {
            return timeOfUsePrice(strategy, current, previous, consumption, ratio, plus);
        }
        if (Integer.valueOf(3).equals(strategy.getPriceType())) {
            return tieredPrice(strategy, consumption, plus, consumptionBeforePeriod);
        }
        throw new IllegalStateException("不支持的费率策略类型，strategyId=" + strategy.getId());
    }

    private BillingPrice fixedPrice(BigDecimal base, BigDecimal plus, BigDecimal consumption,
                                    Long strategyId, Integer priceType, String billingType) {
        BigDecimal total = base.add(plus);
        return new BillingPrice(base, plus, total, consumption.multiply(total),
                strategyId, priceType, billingType, Map.of("basePrice", base));
    }

    private BillingPrice timeOfUsePrice(PriceStrategy strategy, MeterReading current, MeterReading previous,
                                        BigDecimal consumption, BigDecimal ratio, BigDecimal plus) {
        List<JsonNode> slots = readStrategyRows(strategy);
        Map<String, BigDecimal> prices = new HashMap<>();
        for (JsonNode slot : slots) {
            prices.put(normalizeSlot(slot.path("slotName").asText(slot.path("type").asText())),
                    slot.path("price").decimalValue());
        }
        Map<String, BigDecimal> rawDeltas = new HashMap<>();
        putPositiveDelta(rawDeltas, "SHARP", current.getKwhPeak(), previous == null ? null : previous.getKwhPeak());
        putPositiveDelta(rawDeltas, "PEAK", current.getKwhBusy(), previous == null ? null : previous.getKwhBusy());
        putPositiveDelta(rawDeltas, "FLAT", current.getKwhNormal(), previous == null ? null : previous.getKwhNormal());
        putPositiveDelta(rawDeltas, "VALLEY", current.getKwhIdle(), previous == null ? null : previous.getKwhIdle());

        BigDecimal baseCost = BigDecimal.ZERO;
        BigDecimal slottedConsumption = BigDecimal.ZERO;
        Map<String, Object> detail = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : rawDeltas.entrySet()) {
            BigDecimal price = prices.get(entry.getKey());
            if (price == null) continue;
            BigDecimal usage = entry.getValue().multiply(ratio);
            baseCost = baseCost.add(usage.multiply(price));
            slottedConsumption = slottedConsumption.add(usage);
            detail.put(entry.getKey().toLowerCase(Locale.ROOT) + "Consumption", usage);
        }
        if (slottedConsumption.signum() == 0) {
            JsonNode active = findActiveSlot(slots, current.getTimestamp() == null
                    ? LocalDateTime.now().getHour() : current.getTimestamp().getHour());
            BigDecimal price = active.path("price").decimalValue();
            baseCost = consumption.multiply(price);
            slottedConsumption = consumption;
            detail.put("fallbackSlot", active.path("slotName").asText(active.path("type").asText()));
        }
        BigDecimal effectiveBase = baseCost.divide(slottedConsumption, 8, RoundingMode.HALF_UP);
        BigDecimal cost = baseCost.add(consumption.multiply(plus));
        return new BillingPrice(effectiveBase, plus, effectiveBase.add(plus), cost,
                strategy.getId(), strategy.getPriceType(), "TIME_OF_USE", detail);
    }

    private BillingPrice tieredPrice(PriceStrategy strategy, BigDecimal consumption, BigDecimal plus,
                                     BigDecimal consumptionBeforePeriod) {
        List<JsonNode> tiers = new ArrayList<>(readStrategyRows(strategy));
        tiers.sort(Comparator.comparing(n -> n.hasNonNull("amount")
                ? n.get("amount").decimalValue() : new BigDecimal("999999999999")));
        BigDecimal remaining = consumption;
        BigDecimal cursor = consumptionBeforePeriod == null ? BigDecimal.ZERO : consumptionBeforePeriod;
        BigDecimal previousLimit = BigDecimal.ZERO;
        BigDecimal baseCost = BigDecimal.ZERO;
        List<Map<String, Object>> detailRows = new ArrayList<>();
        for (JsonNode tier : tiers) {
            if (remaining.signum() <= 0) break;
            BigDecimal price = tier.path("price").decimalValue();
            BigDecimal upperLimit = tier.hasNonNull("amount") ? tier.get("amount").decimalValue() : null;
            BigDecimal band = upperLimit == null ? remaining
                    : upperLimit.subtract(cursor.max(previousLimit)).max(BigDecimal.ZERO);
            if (band.signum() < 0) throw new IllegalStateException("阶梯用量上限必须递增，strategyId=" + strategy.getId());
            BigDecimal used = remaining.min(band);
            baseCost = baseCost.add(used.multiply(price));
            detailRows.add(Map.of("consumption", used, "price", price));
            remaining = remaining.subtract(used);
            cursor = cursor.add(used);
            if (tier.hasNonNull("amount")) previousLimit = tier.get("amount").decimalValue();
        }
        if (remaining.signum() > 0) {
            throw new IllegalStateException("阶梯费率未覆盖全部用量，strategyId=" + strategy.getId());
        }
        BigDecimal effectiveBase = baseCost.divide(consumption, 8, RoundingMode.HALF_UP);
        BigDecimal cost = baseCost.add(consumption.multiply(plus));
        return new BillingPrice(effectiveBase, plus, effectiveBase.add(plus), cost,
                strategy.getId(), strategy.getPriceType(), "TIERED",
                Map.of("consumptionBeforePeriod", consumptionBeforePeriod, "tiers", detailRows));
    }

    private List<JsonNode> readStrategyRows(PriceStrategy strategy) {
        try {
            JsonNode root = objectMapper.readTree(strategy.getTimeSlots());
            if (root == null || !root.isArray() || root.isEmpty()) throw new IllegalArgumentException();
            List<JsonNode> rows = new ArrayList<>();
            root.forEach(rows::add);
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("费率策略内容无效，strategyId=" + strategy.getId(), e);
        }
    }

    private JsonNode findActiveSlot(List<JsonNode> slots, int hour) {
        return slots.stream().filter(slot -> {
            int start = slot.path("startHour").asInt(-1);
            int end = slot.path("endHour").asInt(-1);
            return end == 24 ? hour >= start : end > start ? hour >= start && hour < end : hour >= start || hour < end;
        }).findFirst().orElseThrow(() -> new IllegalStateException("分时费率未覆盖当前时段"));
    }

    private String normalizeSlot(String value) {
        return switch (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) {
            case "尖", "SHARP" -> "SHARP";
            case "峰", "PEAK" -> "PEAK";
            case "平", "FLAT" -> "FLAT";
            case "谷", "VALLEY" -> "VALLEY";
            default -> throw new IllegalStateException("未知分时类型：" + value);
        };
    }

    private void putPositiveDelta(Map<String, BigDecimal> target, String key,
                                  BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return;
        BigDecimal delta = current.subtract(previous);
        if (delta.signum() > 0) target.put(key, delta);
    }

    private String buildTraceDetail(BigDecimal previousReading, BigDecimal currentReading,
                                    BigDecimal rawConsumption, BigDecimal billedConsumption,
                                    Long sourceMeterId, Long accountMeterId, BillingPrice price) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("previousReading", previousReading);
        detail.put("currentReading", currentReading);
        detail.put("rawConsumption", rawConsumption);
        detail.put("consumption", billedConsumption);
        detail.put("sourceMeterId", sourceMeterId);
        detail.put("accountMeterId", accountMeterId);
        detail.put("billingType", price.billingType());
        detail.put("priceBreakdown", price.detail());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            throw new IllegalStateException("计费轨迹序列化失败", e);
        }
    }

    private Map<String, Object> segmentDetail(SegmentComputation segment) {
        Map<String, Object> row = new HashMap<>();
        row.put("periodStart", segment.periodStart() == null ? null : segment.periodStart().toString());
        row.put("periodEnd", segment.periodEnd() == null ? null : segment.periodEnd().toString());
        row.put("previousReading", segment.previousReading());
        row.put("currentReading", segment.currentReading());
        row.put("rawConsumption", segment.rawConsumption());
        row.put("billedConsumption", segment.billedConsumption());
        row.put("strategyId", segment.price().rateConfigId());
        row.put("billingType", segment.price().billingType());
        row.put("unitPrice", segment.price().totalPrice());
        row.put("chargeAmount", segment.price().cost());
        return row;
    }

    private void persistBillingSegment(String settlementNo, BillingTarget target, Meter sourceMeter,
                                       BigDecimal ratio, BillingDimensions dimensions,
                                       SegmentComputation segment) {
        PriceStrategy strategy = priceStrategyMapper.selectById(segment.price().rateConfigId());
        BillingSegment row = new BillingSegment();
        row.setSettlementNo(settlementNo);
        row.setVirtualMeterId(target.accountMeter().getId());
        row.setSourceMeterId(sourceMeter.getId());
        row.setCustomerId(target.customerId());
        row.setBillingDate((segment.periodEnd() == null ? LocalDateTime.now() : segment.periodEnd()).toLocalDate());
        row.setPeriodStart(segment.periodStart()); row.setPeriodEnd(segment.periodEnd());
        row.setPreviousReading(segment.previousReading()); row.setCurrentReading(segment.currentReading());
        row.setRawConsumption(segment.rawConsumption()); row.setBilledConsumption(segment.billedConsumption());
        row.setCtRatio(defaultRatio(sourceMeter.getCtRatio())); row.setPtRatio(defaultRatio(sourceMeter.getPtRatio()));
        row.setStrategyId(segment.price().rateConfigId());
        if (strategy != null) {
            row.setStrategyCode(strategy.getStrategyCode());
            row.setStrategyVersion(strategy.getVersionNo());
        }
        row.setPriceType(segment.price().priceType()); row.setUnitPrice(segment.price().basePrice());
        row.setPlusPrice(segment.price().plusPrice()); row.setChargeAmount(segment.price().cost());
        row.setProjectId(dimensions.projectId()); row.setProjectName(dimensions.projectName());
        row.setAreaGroupId(dimensions.areaGroupId()); row.setAreaName(dimensions.areaName());
        row.setCategoryGroupId(dimensions.categoryGroupId()); row.setCategoryName(dimensions.categoryName());
        try { row.setDetailJson(objectMapper.writeValueAsString(segment.price().detail())); }
        catch (Exception e) { throw new IllegalStateException("计费分段序列化失败", e); }
        row.setCreateTime(LocalDateTime.now()); row.setIsDeleted(0);
        billingSegmentMapper.insert(row);
    }

    private BillingDimensions resolveDimensions(Meter meter) {
        Long projectId = meter.getProId();
        String projectName = meter.getProName();
        Long areaId = null, categoryId = null;
        String areaName = null, categoryName = null;
        Long groupId = meter.getGroupId();
        int guard = 0;
        while (groupId != null && guard++ < 32) {
            MeterGroup group = meterGroupMapper.selectById(groupId);
            if (group == null) break;
            String type = group.getDimensionType() == null ? "NORMAL" : group.getDimensionType();
            if ("PROJECT".equals(type) && projectName == null) {
                projectId = group.getId(); projectName = group.getGroupName();
            } else if ("AREA".equals(type) && areaId == null) {
                areaId = group.getId(); areaName = group.getGroupName();
            } else if ("CATEGORY".equals(type) && categoryId == null) {
                categoryId = group.getId(); categoryName = group.getGroupName();
            }
            groupId = group.getParentId();
        }
        if (projectId != null && projectName == null) {
            Project project = projectMapper.selectById(projectId);
            if (project != null) projectName = project.getProjectName();
        }
        return new BillingDimensions(projectId, projectName, areaId, areaName, categoryId, categoryName);
    }

    private BigDecimal defaultRatio(BigDecimal ratio) {
        return ratio != null ? ratio : BigDecimal.ONE;
    }

    private record BillingTarget(Meter accountMeter, Long customerId) {
    }

    private record SegmentComputation(LocalDateTime periodStart, LocalDateTime periodEnd,
                                      BigDecimal previousReading, BigDecimal currentReading,
                                      BigDecimal rawConsumption, BigDecimal billedConsumption,
                                      BillingPrice price) {}

    private record BillingDimensions(Long projectId, String projectName, Long areaGroupId,
                                     String areaName, Long categoryGroupId, String categoryName) {}

    private record BillingPrice(BigDecimal basePrice, BigDecimal plusPrice, BigDecimal totalPrice,
                                BigDecimal cost, Long rateConfigId, Integer priceType,
                                String billingType, Map<String, ?> detail) {
    }

    /** 解析 startVal（String 或数字）为 BigDecimal */
    private BigDecimal parseStartVal(String startVal) {
        if (startVal == null || startVal.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(startVal.trim());
        } catch (NumberFormatException e) {
            log.warn("[自动扣费] startVal 无法解析为数字: {}", startVal);
            return BigDecimal.ZERO;
        }
    }

    private String generateFlowNo() {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "EC" + shortTs + shortUuid;
    }
}
