package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.dto.BillingMeterSummaryVO;
import com.competition.energycontrollerplatform.dto.BillingMonthlyMeterVO;
import com.competition.energycontrollerplatform.dto.BillingReportVO;
import com.competition.energycontrollerplatform.dto.BillingYearlyMeterDetailVO;
import com.competition.energycontrollerplatform.dto.CustomerComprehensiveVO;
import com.competition.energycontrollerplatform.dto.DailyElectricityVO;
import com.competition.energycontrollerplatform.dto.DeletedMeterReportVO;
import com.competition.energycontrollerplatform.dto.ElectricLoadCurveVO;
import com.competition.energycontrollerplatform.dto.ElectricLoadVO;
import com.competition.energycontrollerplatform.dto.MonthlyElecSummaryVO;
import com.competition.energycontrollerplatform.dto.MonthlyMeterDetailVO;
import com.competition.energycontrollerplatform.dto.ReportSummaryVO;
import com.competition.energycontrollerplatform.dto.TimeOfUseReportVO;
import com.competition.energycontrollerplatform.dto.YearlyMeterDetailVO;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterGroup;
import com.competition.energycontrollerplatform.entity.ReportDailyElectricity;
import com.competition.energycontrollerplatform.entity.ShadowMeter;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterGroupMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.ReportDailyElectricityMapper;
import com.competition.energycontrollerplatform.mapper.ShadowMeterMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表统计服务实现。
 * <p>
 * 所有查询均走预聚合表 t_report_daily_electricity，不触碰 t_meter_reading 原始表。
 * 非 ADMIN 角色查询时强制注入当前商户 ID。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportDailyElectricityMapper reportDailyElectricityMapper;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final MeterGroupMapper meterGroupMapper;
    private final ShadowMeterMapper shadowMeterMapper;

    // ──────────────────────── 日报分页明细 ────────────────────────

    @Override
    public IPage<DailyElectricityVO> listDailyElectricity(int pageNum, int pageSize,
                                                           LocalDate startDate, LocalDate endDate,
                                                           Long customerId, Long groupId) {
        // 权限隔离
        Long effectiveCustomerId = resolveCustomerId(customerId);

        LambdaQueryWrapper<ReportDailyElectricity> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(ReportDailyElectricity::getReportDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ReportDailyElectricity::getReportDate, endDate);
        }
        if (effectiveCustomerId != null) {
            wrapper.eq(ReportDailyElectricity::getCustomerId, effectiveCustomerId);
        }
        if (groupId != null) {
            wrapper.eq(ReportDailyElectricity::getGroupId, groupId);
        }
        wrapper.orderByDesc(ReportDailyElectricity::getReportDate)
               .orderByDesc(ReportDailyElectricity::getElectricityConsumed);

        Page<ReportDailyElectricity> page = new Page<>(pageNum, pageSize);
        IPage<ReportDailyElectricity> result = reportDailyElectricityMapper.selectPage(page, wrapper);

        // 1. 实体 → VO（基础字段）
        List<DailyElectricityVO> voList = new ArrayList<>(result.getRecords().size());
        for (ReportDailyElectricity entity : result.getRecords()) {
            voList.add(toVO(entity));
        }

        // 2. 批量补充关联字段（groupName, deviceNo, virtualMeter, 反向占比计算）
        enrichDailyElectricityVOs(voList);

        // 3. 构造新的分页对象
        Page<DailyElectricityVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ──────────────────────── 日报汇总 ────────────────────────

    @Override
    public List<ReportSummaryVO> sumDailyElectricity(LocalDate startDate, LocalDate endDate,
                                                      Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumByDate(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToSummary(rows);
    }

    // ──────────────────────── 月报汇总 ────────────────────────

    @Override
    public List<ReportSummaryVO> sumMonthlyElectricity(LocalDate startDate, LocalDate endDate,
                                                        Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumByMonth(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToSummary(rows);
    }

    // ──────────────────────── 用电量月报表明细（按表计+每日用量） ────────────────────────

    @Override
    public List<MonthlyMeterDetailVO> listMonthlyMeterDetail(LocalDate startDate, LocalDate endDate,
                                                              Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.monthlyDetailByMeter(
                startDate, endDate, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 1. 按 meterId 分组，构建每日用量 Map
        Map<Long, MonthlyMeterDetailVO> voMap = new LinkedHashMap<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd");

        for (Map<String, Object> row : rows) {
            Long meterId = toLongValue(row.get("meterId"));
            MonthlyMeterDetailVO vo = voMap.computeIfAbsent(meterId, id -> {
                MonthlyMeterDetailVO v = new MonthlyMeterDetailVO();
                v.setMeterId(id);
                v.setDeviceNo(toStringValue(row.get("deviceNo")));
                v.setMeterType(toStringValue(row.get("meterType")));
                v.setGroupName(toStringValue(row.get("groupName")));
                v.setCustomerName(toStringValue(row.get("customerName")));
                v.setDailyUsage(new LinkedHashMap<>());
                v.setTotalUsage(BigDecimal.ZERO);
                return v;
            });

            // 解析日期 → "MM-dd" 格式
            Object dateObj = row.get("reportDate");
            LocalDate reportDate = null;
            if (dateObj instanceof java.sql.Date sd) {
                reportDate = sd.toLocalDate();
            } else if (dateObj instanceof LocalDate ld) {
                reportDate = ld;
            }
            if (reportDate != null) {
                String dayKey = reportDate.format(dayFmt);
                BigDecimal usage = convertToBigDecimal(row.get("dailyUsage"));
                vo.getDailyUsage().put(dayKey, usage);
                vo.setTotalUsage(vo.getTotalUsage().add(usage));
            }
        }

        List<MonthlyMeterDetailVO> result = new ArrayList<>(voMap.values());

        // 2. 批量补充虚拟表信息
        enrichMonthlyDetailWithVirtualMeters(result);

        log.info("[用电量月报表明细] startDate={}, endDate={}, customerId={}, groupId={}, meters={}",
                startDate, endDate, effectiveCustomerId, groupId, result.size());
        return result;
    }

    /**
     * 为月报表明细 VO 补充虚拟表信息（虚拟表号、虚拟表商户名）。
     */
    private void enrichMonthlyDetailWithVirtualMeters(List<MonthlyMeterDetailVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> meterIds = voList.stream()
                .map(MonthlyMeterDetailVO::getMeterId)
                .distinct()
                .toList();

        // 批量查询影子表（parent → child 映射）
        LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
        smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                 .eq(ShadowMeter::getIsDeleted, 0)
                 .eq(ShadowMeter::getStatus, 1);
        List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
        Map<Long, ShadowMeter> shadowByParent = shadows.stream()
                .collect(Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));

        if (shadowByParent.isEmpty()) return;

        // 查询子表计信息
        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        // 填充虚拟表信息
        for (MonthlyMeterDetailVO vo : voList) {
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
        }
    }

    // ──────────────────────── 客户综合报表 ────────────────────────

    @Override
    public List<CustomerComprehensiveVO> getCustomerComprehensiveReport(String startDate, String endDate,
                                                                         Long customerId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.customerComprehensiveReport(
                startDate, endDate, effectiveCustomerId);

        List<CustomerComprehensiveVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            CustomerComprehensiveVO vo = new CustomerComprehensiveVO();
            vo.setCustomerId((Long) row.get("customerId"));
            vo.setCustomerName((String) row.get("customerName"));
            vo.setCurrentBalance((BigDecimal) row.get("currentBalance"));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setTotalRecharge(convertToBigDecimal(row.get("totalRecharge")));
            vo.setTotalCharge(convertToBigDecimal(row.get("totalCharge")));
            result.add(vo);
        }

        log.info("[客户综合报表] customerId={}, rows={}", effectiveCustomerId, result.size());
        return result;
    }

    // ──────────────────────── 年度聚合（§五.7） ────────────────────────

    @Override
    public List<ReportSummaryVO> sumYearlyElectricity(LocalDate startDate, LocalDate endDate,
                                                       Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumByYear(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToSummary(rows);
    }

    // ──────────────────────── 复费率分时日报（§五.4） ────────────────────────

    @Override
    public List<TimeOfUseReportVO> sumTimeOfUseDaily(LocalDate startDate, LocalDate endDate,
                                                      Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumTimeOfUseByDate(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToTimeOfUse(rows, true);
    }

    @Override
    public List<TimeOfUseReportVO> sumTimeOfUseDailyDetail(LocalDate startDate, LocalDate endDate,
                                                            Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumTimeOfUseByDateDetail(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToTimeOfUseDetail(rows);
    }

    // ──────────────────────── 复费率分时月报（§五.5） ────────────────────────

    @Override
    public List<TimeOfUseReportVO> sumTimeOfUseMonthly(LocalDate startDate, LocalDate endDate,
                                                        Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumTimeOfUseByMonth(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToTimeOfUse(rows, false);
    }

    // ──────────────────────── 计费金额日报（§五.8） ────────────────────────

    @Override
    public List<BillingReportVO> sumBillingDaily(LocalDate startDate, LocalDate endDate,
                                                  Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumBillingByDate(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToBilling(rows);
    }

    // ──────────────────────── 计费金额月报（§五.9） ────────────────────────

    @Override
    public List<BillingReportVO> sumBillingMonthly(LocalDate startDate, LocalDate endDate,
                                                    Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumBillingByMonth(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToBilling(rows);
    }

    // ──────────────────────── 计费金额年报（§五.10） ────────────────────────

    @Override
    public List<BillingReportVO> sumBillingYearly(LocalDate startDate, LocalDate endDate,
                                                   Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumBillingByYear(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToBilling(rows);
    }

    // ──────────────────────── 用电负荷查询（§五.12） ────────────────────────

    @Override
    public List<ElectricLoadVO> getDailyElectricLoad(LocalDate startDate, LocalDate endDate,
                                                      Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.electricLoadByDate(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToElectricLoad(rows);
    }

    @Override
    public List<ElectricLoadVO> getMonthlyElectricLoad(LocalDate startDate, LocalDate endDate,
                                                        Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.electricLoadByMonth(
                startDate, endDate, effectiveCustomerId, groupId);

        return mapToElectricLoad(rows);
    }

    // ──────────────────────── 用电负荷曲线（§五.12 曲线图） ────────────────────────

    @Override
    public List<ElectricLoadCurveVO> getElectricLoadCurve(LocalDate startDate, LocalDate endDate,
                                                           Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.electricLoadCurve(
                startDate, endDate, effectiveCustomerId, groupId);

        List<ElectricLoadCurveVO> result = mapToElectricLoadCurve(rows);

        log.info("[用电负荷曲线图] startDate={}, endDate={}, customerId={}, groupId={}, points={}",
                startDate, endDate, effectiveCustomerId, groupId, result.size());
        return result;
    }

    // ──────────────────────── 已删除表计月报 ────────────────────────

    @Override
    public List<DeletedMeterReportVO> getDeletedMeterMonthlyReport(String statMonth, Long merchantId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(merchantId);

        // 1. 主查询：已删除电表的月度汇总 + 维度信息 + 虚拟表
        List<Map<String, Object>> rows = reportDailyElectricityMapper.deletedMeterMonthlyReport(
                statMonth, effectiveCustomerId, groupId);

        // 2. 逐条映射基础字段
        List<DeletedMeterReportVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            DeletedMeterReportVO vo = new DeletedMeterReportVO();
            vo.setStatMonth(toStringValue(row.get("statMonth")));
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setMeterId(toLongValue(row.get("meterId")));
            vo.setMerchantName(toStringValue(row.get("merchantName")));
            vo.setProjectName(toStringValue(row.get("projectName")));
            vo.setGroupName(toStringValue(row.get("groupName")));
            vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
            vo.setPeakUsage(convertToBigDecimal(row.get("peakUsage")));
            vo.setBusyUsage(convertToBigDecimal(row.get("busyUsage")));
            vo.setNormalUsage(convertToBigDecimal(row.get("normalUsage")));
            vo.setIdleUsage(convertToBigDecimal(row.get("idleUsage")));

            // 设备类型：1=电表, 0=水表
            Object meterTypeObj = row.get("meterType");
            if (meterTypeObj instanceof Number n) {
                vo.setDeviceType(n.intValue() == 1 ? "电表" : "水表");
            } else {
                vo.setDeviceType("电表");
            }

            // 虚拟表信息
            vo.setVirtualMeterNo(toStringValue(row.get("virtualMeterNo")));
            vo.setVirtualMerchantName(toStringValue(row.get("virtualMerchantName")));

            // 删除时间
            Object deletedTimeObj = row.get("deletedTime");
            if (deletedTimeObj instanceof java.sql.Timestamp ts) {
                vo.setDeletedTime(ts.toLocalDateTime());
            } else if (deletedTimeObj instanceof java.time.LocalDateTime ldt) {
                vo.setDeletedTime(ldt);
            }

            // 统计天数
            Object dailyCountObj = row.get("dailyCount");
            if (dailyCountObj instanceof Number n) {
                vo.setDailyCount(n.intValue());
            }
            vo.setDataSource(toStringValue(row.get("dataSource")));
            result.add(vo);
        }

        // 3. 查询每日用量明细并填充 dailyUsage Map
        if (statMonth != null && !statMonth.isEmpty() && !result.isEmpty()) {
            List<Map<String, Object>> dailyRows =
                    reportDailyElectricityMapper.dailyConsumptionForDeletedMeters(statMonth);

            // 按 meterId 构建快速查找 Map
            java.util.Map<Long, DeletedMeterReportVO> voMap = new java.util.HashMap<>();
            for (DeletedMeterReportVO vo : result) {
                voMap.put(vo.getMeterId(), vo);
            }

            for (Map<String, Object> dr : dailyRows) {
                Long meterId = toLongValue(dr.get("meterId"));
                String dayKey = toStringValue(dr.get("dayKey"));
                BigDecimal consumed = convertToBigDecimal(dr.get("dailyConsumed"));

                DeletedMeterReportVO vo = voMap.get(meterId);
                if (vo != null && dayKey != null) {
                    vo.putDaily(dayKey, consumed);
                }
            }
        }

        log.info("[已删除电表月报] statMonth={}, rows={}", statMonth, result.size());
        return result;
    }

    // ──────────────────────── 月度用电量汇总明细（按表计×月度，含正向/反向/余额） ────────────────────────

    @Override
    public List<MonthlyElecSummaryVO> listMonthlyElecSummary(String statMonth, Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.monthlySummaryByMeter(
                statMonth, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        List<MonthlyElecSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            MonthlyElecSummaryVO vo = new MonthlyElecSummaryVO();
            vo.setMeterId(toLongValue(row.get("meterId")));
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setCustomerName(toStringValue(row.get("customerName")));
            vo.setGroupName(toStringValue(row.get("groupName")));
            vo.setVirtualMeterNo(toStringValue(row.get("virtualMeterNo")));
            vo.setVirtualMerchantName(toStringValue(row.get("virtualMerchantName")));
            vo.setMeterType(toStringValue(row.get("meterType")));
            vo.setStatMonth(toStringValue(row.get("statMonth")));
            // 正向有功
            vo.setForwardStartReading(convertToBigDecimal(row.get("forwardStartReading")));
            vo.setForwardStartTime(toLocalDateTime(row.get("forwardStartTime")));
            vo.setForwardEndReading(convertToBigDecimal(row.get("forwardEndReading")));
            vo.setForwardEndTime(toLocalDateTime(row.get("forwardEndTime")));
            vo.setForwardUsage(convertToBigDecimal(row.get("forwardUsage")));
            // 反向有功
            vo.setReverseStartReading(convertToBigDecimal(row.get("reverseStartReading")));
            vo.setReverseStartTime(toLocalDateTime(row.get("reverseStartTime")));
            vo.setReverseEndReading(convertToBigDecimal(row.get("reverseEndReading")));
            vo.setReverseEndTime(toLocalDateTime(row.get("reverseEndTime")));
            vo.setReverseUsage(convertToBigDecimal(row.get("reverseUsage")));
            vo.setReverseRatio(convertToBigDecimal(row.get("reverseRatio")));
            // 总起止示数
            vo.setTotalStartReading(convertToBigDecimal(row.get("totalStartReading")));
            vo.setTotalEndReading(convertToBigDecimal(row.get("totalEndReading")));
            // 总电量
            vo.setTotalEnergy(convertToBigDecimal(row.get("totalEnergy")));
            // 余额
            vo.setPreviousBalance(convertToBigDecimal(row.get("previousBalance")));
            vo.setCurrentBalance(convertToBigDecimal(row.get("currentBalance")));
            // 数据更新时间
            vo.setDataUpdateTime(toLocalDateTime(row.get("dataUpdateTime")));

            result.add(vo);
        }

        log.info("[月度用电量汇总明细] statMonth={}, customerId={}, groupId={}, rows={}",
                statMonth, effectiveCustomerId, groupId, result.size());
        return result;
    }

    // ──────────────────────── 年度用电量汇总（按表计×12月明细） ────────────────────────

    @Override
    public List<YearlyMeterDetailVO> getYearlyMeterDetail(String year, Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.yearlyDetailByMeter(
                year, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 1. 基础字段映射
        List<YearlyMeterDetailVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            YearlyMeterDetailVO vo = new YearlyMeterDetailVO();
            vo.setMeterId(toLongValue(row.get("meterId")));
            vo.setYear(year);
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setCustomerName(toStringValue(row.get("customerName")));
            vo.setMeterType(toStringValue(row.get("meterType")));
            vo.setMonth01(convertToBigDecimal(row.get("month01")));
            vo.setMonth02(convertToBigDecimal(row.get("month02")));
            vo.setMonth03(convertToBigDecimal(row.get("month03")));
            vo.setMonth04(convertToBigDecimal(row.get("month04")));
            vo.setMonth05(convertToBigDecimal(row.get("month05")));
            vo.setMonth06(convertToBigDecimal(row.get("month06")));
            vo.setMonth07(convertToBigDecimal(row.get("month07")));
            vo.setMonth08(convertToBigDecimal(row.get("month08")));
            vo.setMonth09(convertToBigDecimal(row.get("month09")));
            vo.setMonth10(convertToBigDecimal(row.get("month10")));
            vo.setMonth11(convertToBigDecimal(row.get("month11")));
            vo.setMonth12(convertToBigDecimal(row.get("month12")));
            vo.setSubtotal(convertToBigDecimal(row.get("subtotal")));
            result.add(vo);
        }

        // 2. 批量补充虚拟表信息
        enrichYearlyDetailWithVirtualMeters(result);

        log.info("[年度用电量汇总] year={}, customerId={}, groupId={}, meters={}",
                year, effectiveCustomerId, groupId, result.size());
        return result;
    }

    /**
     * 为年度汇总 VO 补充虚拟表信息（虚拟表号、虚拟表商户名）。
     */
    private void enrichYearlyDetailWithVirtualMeters(List<YearlyMeterDetailVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> meterIds = voList.stream()
                .map(YearlyMeterDetailVO::getMeterId)
                .distinct()
                .toList();

        // 批量查询影子表
        LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
        smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                 .eq(ShadowMeter::getIsDeleted, 0)
                 .eq(ShadowMeter::getStatus, 1);
        List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
        Map<Long, ShadowMeter> shadowByParent = shadows.stream()
                .collect(Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));

        if (shadowByParent.isEmpty()) return;

        // 批量查询子表计信息
        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        // 逐个填充
        for (YearlyMeterDetailVO vo : voList) {
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
        }
    }

    // ──────────────────────── 计费电量年报明细（按表计+年度/本月汇总宽表） ────────────────────────

    @Override
    public List<BillingYearlyMeterDetailVO> getBillingYearlyMeterDetail(
            String year, String statMonth, Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.billingYearlyDetailByMeter(
                year, statMonth, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 1. 基础字段映射
        List<BillingYearlyMeterDetailVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            BillingYearlyMeterDetailVO vo = new BillingYearlyMeterDetailVO();
            vo.setMeterId(toLongValue(row.get("meterId")));
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setCustomerName(toStringValue(row.get("customerName")));
            vo.setGroupName(toStringValue(row.get("groupName")));
            vo.setYearAmount(convertToBigDecimal(row.get("yearAmount")));
            vo.setYearUsage(convertToBigDecimal(row.get("yearUsage")));
            vo.setMonthAmount(convertToBigDecimal(row.get("monthAmount")));
            vo.setMonthUsage(convertToBigDecimal(row.get("monthUsage")));
            vo.setCurrentBalance(convertToBigDecimal(row.get("currentBalance")));
            vo.setPreviousBalance(convertToBigDecimal(row.get("previousBalance")));
            result.add(vo);
        }

        // 2. 批量补充虚拟表信息
        enrichBillingYearlyDetailWithVirtualMeters(result);

        log.info("[计费电量年报明细] year={}, month={}, customerId={}, groupId={}, meters={}",
                year, statMonth, effectiveCustomerId, groupId, result.size());
        return result;
    }

    /**
     * 为计费年报 VO 补充虚拟表信息（虚拟表号、虚拟表商户名）。
     */
    private void enrichBillingYearlyDetailWithVirtualMeters(List<BillingYearlyMeterDetailVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> meterIds = voList.stream()
                .map(BillingYearlyMeterDetailVO::getMeterId)
                .distinct()
                .toList();

        // 批量查询影子表
        LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
        smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                 .eq(ShadowMeter::getIsDeleted, 0)
                 .eq(ShadowMeter::getStatus, 1);
        List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
        Map<Long, ShadowMeter> shadowByParent = shadows.stream()
                .collect(Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));

        if (shadowByParent.isEmpty()) return;

        // 批量查询子表计信息
        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        // 逐个填充
        for (BillingYearlyMeterDetailVO vo : voList) {
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
        }
    }

    // ──────────────────────── 计费电量汇总（按表计维度） ────────────────────────

    @Override
    public List<BillingMeterSummaryVO> getBillingMeterSummary(LocalDate startDate, LocalDate endDate,
                                                               Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.billingByMeter(
                startDate, endDate, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 1. 基础字段映射
        List<BillingMeterSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            BillingMeterSummaryVO vo = new BillingMeterSummaryVO();
            vo.setMeterId(toLongValue(row.get("meterId")));
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setCustomerName(toStringValue(row.get("customerName")));
            vo.setTotalChargeAmount(convertToBigDecimal(row.get("totalChargeAmount")));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));

            // 计费均价 = 计费金额 / 计费电量
            BigDecimal charge = vo.getTotalChargeAmount();
            BigDecimal consumed = vo.getTotalConsumed();
            if (consumed.compareTo(BigDecimal.ZERO) > 0) {
                vo.setAveragePrice(charge.divide(consumed, 4, java.math.RoundingMode.HALF_UP));
            } else {
                vo.setAveragePrice(BigDecimal.ZERO);
            }
            result.add(vo);
        }

        // 2. 批量补充虚拟表信息
        enrichBillingMeterSummaryWithVirtualMeters(result);

        log.info("[计费电量汇总] startDate={}, endDate={}, customerId={}, groupId={}, meters={}",
                startDate, endDate, effectiveCustomerId, groupId, result.size());
        return result;
    }

    // ──────────────────────── 计费电量月报表明细（按表计+每日计费） ────────────────────────

    @Override
    public List<BillingMonthlyMeterVO> getBillingMonthlyMeterDetail(LocalDate startDate, LocalDate endDate,
                                                                     Long customerId, Long groupId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        List<Map<String, Object>> rows = reportDailyElectricityMapper.billingMonthlyDetailByMeter(
                startDate, endDate, effectiveCustomerId, groupId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 1. 按 meterId 分组，构建每日计费 Map
        Map<Long, BillingMonthlyMeterVO> voMap = new LinkedHashMap<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd");

        for (Map<String, Object> row : rows) {
            Long meterId = toLongValue(row.get("meterId"));
            BillingMonthlyMeterVO vo = voMap.computeIfAbsent(meterId, id -> {
                BillingMonthlyMeterVO v = new BillingMonthlyMeterVO();
                v.setMeterId(id);
                v.setDeviceNo(toStringValue(row.get("deviceNo")));
                v.setDeviceType(toStringValue(row.get("deviceType")));
                v.setGroupName(toStringValue(row.get("groupName")));
                v.setMerchantName(toStringValue(row.get("merchantName")));
                v.setDailyKwh(new LinkedHashMap<>());
                v.setDailyAmt(new LinkedHashMap<>());
                v.setTotalKwh(BigDecimal.ZERO);
                v.setTotalAmt(BigDecimal.ZERO);
                return v;
            });

            // 解析日期 → "MM-dd" 格式
            Object dateObj = row.get("reportDate");
            LocalDate reportDate = null;
            if (dateObj instanceof java.sql.Date sd) {
                reportDate = sd.toLocalDate();
            } else if (dateObj instanceof LocalDate ld) {
                reportDate = ld;
            }
            if (reportDate != null) {
                String dayKey = reportDate.format(dayFmt);
                BigDecimal kwh = convertToBigDecimal(row.get("dailyKwh"));
                BigDecimal amt = convertToBigDecimal(row.get("dailyAmt"));
                vo.getDailyKwh().put(dayKey, kwh);
                vo.getDailyAmt().put(dayKey, amt);
                vo.setTotalKwh(vo.getTotalKwh().add(kwh));
                vo.setTotalAmt(vo.getTotalAmt().add(amt));
            }
        }

        List<BillingMonthlyMeterVO> result = new ArrayList<>(voMap.values());

        // 2. 批量补充虚拟表信息
        enrichBillingMonthlyDetailWithVirtualMeters(result);

        log.info("[计费电量月报表明细] startDate={}, endDate={}, customerId={}, groupId={}, meters={}",
                startDate, endDate, effectiveCustomerId, groupId, result.size());
        return result;
    }

    /**
     * 为计费月报表明细 VO 补充虚拟表信息（虚拟表号、虚拟表商户名）。
     */
    private void enrichBillingMonthlyDetailWithVirtualMeters(List<BillingMonthlyMeterVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> meterIds = voList.stream()
                .map(BillingMonthlyMeterVO::getMeterId)
                .distinct()
                .toList();

        LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
        smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                 .eq(ShadowMeter::getIsDeleted, 0)
                 .eq(ShadowMeter::getStatus, 1);
        List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
        Map<Long, ShadowMeter> shadowByParent = shadows.stream()
                .collect(Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));

        if (shadowByParent.isEmpty()) return;

        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        for (BillingMonthlyMeterVO vo : voList) {
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
        }
    }

    /**
     * 为计费电量汇总 VO 补充虚拟表信息（虚拟表号、虚拟表商户名）。
     */
    private void enrichBillingMeterSummaryWithVirtualMeters(List<BillingMeterSummaryVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> meterIds = voList.stream()
                .map(BillingMeterSummaryVO::getMeterId)
                .distinct()
                .toList();

        LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
        smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                 .eq(ShadowMeter::getIsDeleted, 0)
                 .eq(ShadowMeter::getStatus, 1);
        List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
        Map<Long, ShadowMeter> shadowByParent = shadows.stream()
                .collect(Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));

        if (shadowByParent.isEmpty()) return;

        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        for (BillingMeterSummaryVO vo : voList) {
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
        }
    }

    // ──────────────────────── 私有 ────────────────────────

    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        return myCustomerId != null ? myCustomerId : -1L;
    }

    private DailyElectricityVO toVO(ReportDailyElectricity entity) {
        DailyElectricityVO vo = new DailyElectricityVO();
        vo.setReportDate(entity.getReportDate());
        vo.setMeterId(entity.getMeterId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setGroupId(entity.getGroupId());
        vo.setElectricityConsumed(entity.getElectricityConsumed());
        vo.setForwardConsumed(entity.getForwardConsumed() != null ? entity.getForwardConsumed() : BigDecimal.ZERO);
        vo.setReverseConsumed(entity.getReverseConsumed() != null ? entity.getReverseConsumed() : BigDecimal.ZERO);
        vo.setPeakConsumed(entity.getPeakConsumed());
        vo.setBusyConsumed(entity.getBusyConsumed());
        vo.setNormalConsumed(entity.getNormalConsumed());
        vo.setIdleConsumed(entity.getIdleConsumed());
        vo.setPeakChargeAmount(entity.getPeakChargeAmount() != null ? entity.getPeakChargeAmount() : BigDecimal.ZERO);
        vo.setBusyChargeAmount(entity.getBusyChargeAmount() != null ? entity.getBusyChargeAmount() : BigDecimal.ZERO);
        vo.setNormalChargeAmount(entity.getNormalChargeAmount() != null ? entity.getNormalChargeAmount() : BigDecimal.ZERO);
        vo.setIdleChargeAmount(entity.getIdleChargeAmount() != null ? entity.getIdleChargeAmount() : BigDecimal.ZERO);
        vo.setChargeAmount(entity.getChargeAmount());


        // 商户名称稍后在 enrich 中批量填充
        return vo;
    }

    /**
     * 批量补充日报表 VO 的关联字段（商户、设备号、分组名、虚拟表、反向占比）。
     */
    private void enrichDailyElectricityVOs(List<DailyElectricityVO> voList) {
        if (voList.isEmpty()) return;

        // 收集所有 customerId
        List<Long> customerIds = voList.stream()
                .map(DailyElectricityVO::getCustomerId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        // 收集所有 meterId
        List<Long> meterIds = voList.stream()
                .map(DailyElectricityVO::getMeterId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        // 收集所有 groupId
        List<Long> groupIds = voList.stream()
                .map(DailyElectricityVO::getGroupId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        // ── 批量查询 t_meter → deviceNo ──
        Map<Long, Meter> meterMap = Map.of();
        if (!meterIds.isEmpty()) {
            List<Meter> meters = meterMapper.selectBatchIds(meterIds);
            meterMap = meters.stream().collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
        }

        // ── 批量查询 t_meter_group → groupName ──
        Map<Long, String> groupNameMap = Map.of();
        if (!groupIds.isEmpty()) {
            List<MeterGroup> groups = meterGroupMapper.selectBatchIds(groupIds);
            groupNameMap = groups.stream().collect(java.util.stream.Collectors.toMap(MeterGroup::getId, MeterGroup::getGroupName, (a, b) -> a));
        }

        // ── 批量查询 t_customer → customerName（避免 N+1） ──
        Map<Long, String> customerNameMap = Map.of();
        if (!customerIds.isEmpty()) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            customerNameMap = customers.stream().collect(java.util.stream.Collectors.toMap(Customer::getId, Customer::getCustomerName, (a, b) -> a));
        }

        // ── 批量查询 t_shadow_meter → virtual meter info ──
        Map<Long, ShadowMeter> shadowByParent = Map.of();
        if (!meterIds.isEmpty()) {
            LambdaQueryWrapper<ShadowMeter> smWrapper = new LambdaQueryWrapper<>();
            smWrapper.in(ShadowMeter::getParentMeterId, meterIds)
                     .eq(ShadowMeter::getIsDeleted, 0)
                     .eq(ShadowMeter::getStatus, 1);
            List<ShadowMeter> shadows = shadowMeterMapper.selectList(smWrapper);
            shadowByParent = shadows.stream().collect(java.util.stream.Collectors.toMap(ShadowMeter::getParentMeterId, s -> s, (a, b) -> a));
        }

        // 收集虚拟表 child meterIds
        List<Long> childMeterIds = shadowByParent.values().stream()
                .map(ShadowMeter::getChildMeterId)
                .distinct()
                .toList();
        Map<Long, Meter> childMeterMap = Map.of();
        Map<Long, Customer> childCustomerMap = Map.of();
        if (!childMeterIds.isEmpty()) {
            List<Meter> childMeters = meterMapper.selectBatchIds(childMeterIds);
            childMeterMap = childMeters.stream().collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
            // 虚拟表的商户
            List<Long> childCustomerIds = childMeters.stream()
                    .map(Meter::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!childCustomerIds.isEmpty()) {
                List<Customer> childCustomers = customerMapper.selectBatchIds(childCustomerIds);
                childCustomerMap = childCustomers.stream().collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
            }
        }

        // ── 逐个填充 ──
        for (DailyElectricityVO vo : voList) {
            // customerName
            if (vo.getCustomerId() != null) {
                vo.setCustomerName(customerNameMap.get(vo.getCustomerId()));
            }
            // deviceNo
            if (vo.getMeterId() != null) {
                Meter meter = meterMap.get(vo.getMeterId());
                if (meter != null) {
                    vo.setDeviceNo(meter.getStealNo());
                }
            }
            // groupName
            if (vo.getGroupId() != null && vo.getGroupId() > 0) {
                vo.setGroupName(groupNameMap.get(vo.getGroupId()));
            }
            // virtual meter
            ShadowMeter shadow = shadowByParent.get(vo.getMeterId());
            if (shadow != null) {
                vo.setVirtualMeterId(shadow.getChildMeterId());
                Meter childMeter = childMeterMap.get(shadow.getChildMeterId());
                if (childMeter != null) {
                    vo.setVirtualMeterNo(childMeter.getStealNo());
                    Customer childCust = childCustomerMap.get(childMeter.getCustomerId());
                    if (childCust != null) {
                        vo.setVirtualMerchantName(childCust.getCustomerName());
                    }
                }
            }
            // reverseRatio = reverseConsumed / (forwardConsumed + reverseConsumed) × 100
            BigDecimal forward = vo.getForwardConsumed() != null ? vo.getForwardConsumed() : BigDecimal.ZERO;
            BigDecimal reverse = vo.getReverseConsumed() != null ? vo.getReverseConsumed() : BigDecimal.ZERO;
            BigDecimal total = forward.add(reverse);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                vo.setReverseRatio(reverse.divide(total, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            } else {
                vo.setReverseRatio(BigDecimal.ZERO);
            }
        }
    }

    private List<ReportSummaryVO> mapToSummary(List<Map<String, Object>> rows) {
        List<ReportSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ReportSummaryVO vo = new ReportSummaryVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setMeterCount(toLongValue(row.get("meterCount")));
            result.add(vo);
        }
        return result;
    }

    /**
     * 映射复费率分时数据。
     *
     * @param rows       SQL 结果行
     * @param hasDate    是否包含 reportDate 字段（日维度=true，月维度=false）
     */
    private List<TimeOfUseReportVO> mapToTimeOfUse(List<Map<String, Object>> rows, boolean hasDate) {
        List<TimeOfUseReportVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            TimeOfUseReportVO vo = new TimeOfUseReportVO();
            if (hasDate && row.get("reportDate") != null) {
                // 日维度：返回具体日期
                Object dateObj = row.get("reportDate");
                if (dateObj instanceof java.sql.Date sd) {
                    vo.setReportDate(sd.toLocalDate());
                } else if (dateObj instanceof java.time.LocalDate ld) {
                    vo.setReportDate(ld);
                }
            }
            vo.setPeriod(toStringValue(row.get(hasDate ? "reportDate" : "period")));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setPeakConsumed(convertToBigDecimal(row.get("peakConsumed")));
            vo.setBusyConsumed(convertToBigDecimal(row.get("busyConsumed")));
            vo.setNormalConsumed(convertToBigDecimal(row.get("normalConsumed")));
            vo.setIdleConsumed(convertToBigDecimal(row.get("idleConsumed")));
            vo.setMeterCount((Long) row.get("meterCount"));
            result.add(vo);
        }
        return result;
    }

    /**
     * 映射复费率分时明细数据（per meter x date，不聚合）。
     * <p>用于复费率计费电量日报表明细表格，包含商户号/表号/各时段电量和电费。</p>
     */
    private List<TimeOfUseReportVO> mapToTimeOfUseDetail(List<Map<String, Object>> rows) {
        List<TimeOfUseReportVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            TimeOfUseReportVO vo = new TimeOfUseReportVO();

            // 日期
            Object dateObj = row.get("reportDate");
            if (dateObj instanceof java.sql.Date sd) {
                vo.setReportDate(sd.toLocalDate());
            } else if (dateObj instanceof java.time.LocalDate ld) {
                vo.setReportDate(ld);
            }

            // 商户号 & 商户名
            vo.setCustomerCode(toStringValue(row.get("customerCode")));
            vo.setCustomerName(toStringValue(row.get("customerName")));

            // 表号 & 表计ID
            vo.setDeviceNo(toStringValue(row.get("deviceNo")));
            vo.setMeterId(toLongValue(row.get("meterId")));

            // period = reportDate 字符串形式
            if (vo.getReportDate() != null) {
                vo.setPeriod(vo.getReportDate().toString());
            }

            // 总用量
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setTotalCharge(convertToBigDecimal(row.get("totalCharge")));

            // 尖
            vo.setPeakConsumed(convertToBigDecimal(row.get("peakConsumed")));
            vo.setPeakCharge(convertToBigDecimal(row.get("peakCharge")));

            // 峰
            vo.setBusyConsumed(convertToBigDecimal(row.get("busyConsumed")));
            vo.setBusyCharge(convertToBigDecimal(row.get("busyCharge")));

            // 平
            vo.setNormalConsumed(convertToBigDecimal(row.get("normalConsumed")));
            vo.setNormalCharge(convertToBigDecimal(row.get("normalCharge")));

            // 谷
            vo.setIdleConsumed(convertToBigDecimal(row.get("idleConsumed")));
            vo.setIdleCharge(convertToBigDecimal(row.get("idleCharge")));

            result.add(vo);
        }
        return result;
    }

    /**
     * 映射计费金额数据，同时计算平均电价。
     */
    private List<BillingReportVO> mapToBilling(List<Map<String, Object>> rows) {
        List<BillingReportVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            BillingReportVO vo = new BillingReportVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setTotalChargeAmount(convertToBigDecimal(row.get("totalChargeAmount")));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setMeterCount(toLongValue(row.get("meterCount")));

            // 平均电价 = 总费用 / 总用量（避免除以0）
            BigDecimal totalCharge = vo.getTotalChargeAmount();
            BigDecimal totalConsumed = vo.getTotalConsumed();
            if (totalConsumed.compareTo(BigDecimal.ZERO) > 0) {
                vo.setAveragePrice(totalCharge.divide(totalConsumed, 4, java.math.RoundingMode.HALF_UP));
            } else {
                vo.setAveragePrice(BigDecimal.ZERO);
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 映射用电负荷数据，同时计算负荷率。
     * <p>负荷率 = 平均负荷 / 最大负荷 × 100%，反映用电均衡程度。</p>
     */
    private List<ElectricLoadVO> mapToElectricLoad(List<Map<String, Object>> rows) {
        List<ElectricLoadVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ElectricLoadVO vo = new ElectricLoadVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setMaxLoad(convertToBigDecimal(row.get("maxLoad")));
            vo.setAverageLoad(convertToBigDecimal(row.get("averageLoad")));
            vo.setMinLoad(convertToBigDecimal(row.get("minLoad")));
            vo.setTotalConsumed(convertToBigDecimal(row.get("totalConsumed")));
            vo.setMeterCount(toLongValue(row.get("meterCount")));

            // dayCount 在上层子查询中是 COUNT(*)，作为 Integer 返回
            Object dayCountObj = row.get("dayCount");
            if (dayCountObj instanceof Number n) {
                vo.setDayCount(n.intValue());
            }

            // 负荷率 = 平均 / 最大 × 100（避免除以0）
            BigDecimal maxLoad = vo.getMaxLoad();
            BigDecimal avgLoad = vo.getAverageLoad();
            if (maxLoad.compareTo(BigDecimal.ZERO) > 0) {
                vo.setLoadRate(avgLoad.divide(maxLoad, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)));
            } else {
                vo.setLoadRate(BigDecimal.ZERO);
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 映射用电负荷曲线数据。
     * <p>将 SQL 结果行映射为 ElectricLoadCurveVO，专用于前端曲线图渲染。</p>
     */
    private List<ElectricLoadCurveVO> mapToElectricLoadCurve(List<Map<String, Object>> rows) {
        List<ElectricLoadCurveVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ElectricLoadCurveVO vo = new ElectricLoadCurveVO();
            vo.setDate(toStringValue(row.get("date")));
            vo.setTotalLoad(convertToBigDecimal(row.get("totalLoad")));
            vo.setPeakLoad(convertToBigDecimal(row.get("peakLoad")));
            vo.setBusyLoad(convertToBigDecimal(row.get("busyLoad")));
            vo.setNormalLoad(convertToBigDecimal(row.get("normalLoad")));
            vo.setIdleLoad(convertToBigDecimal(row.get("idleLoad")));
            vo.setMeterCount(toLongValue(row.get("meterCount")));
            result.add(vo);
        }
        return result;
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String toStringValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate().toString();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate().toString();
        if (value instanceof java.time.LocalDate ld) return ld.toString();
        return value.toString();
    }

    private java.time.LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try {
            return java.time.LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
