package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 报表统计服务接口。
 * <p>
 * 直接从预聚合表 t_report_daily_electricity 读取，
 * 支持按总用户、分组（groupId）、分户（customerId）三维筛选。
 */
public interface ReportService {

    /**
     * 用电量日报表明细分页。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    IPage<DailyElectricityVO> listDailyElectricity(int pageNum, int pageSize,
                                                    LocalDate startDate, LocalDate endDate,
                                                    Long customerId, Long groupId);

    /**
     * 用电量日报表汇总（按日聚合）。
     */
    List<ReportSummaryVO> sumDailyElectricity(LocalDate startDate, LocalDate endDate,
                                              Long customerId, Long groupId);

    /**
     * 用电量月报表汇总（按月聚合）。
     */
    List<ReportSummaryVO> sumMonthlyElectricity(LocalDate startDate, LocalDate endDate,
                                                Long customerId, Long groupId);

    /**
     * 客户综合报表（§五.11：总充值/总用量/总费用/余额）。
     */
    List<CustomerComprehensiveVO> getCustomerComprehensiveReport(String startDate, String endDate, Long customerId);

    // ──────────────── 年度聚合（§五.7） ────────────────

    /**
     * 用电量年报表汇总（按年聚合）。
     */
    List<ReportSummaryVO> sumYearlyElectricity(LocalDate startDate, LocalDate endDate,
                                                Long customerId, Long groupId);

    // ──────────────── 复费率分时报表（§五.4/5） ────────────────

    /**
     * 复费率计费电量日报表（尖/峰/平/谷分列）。
     */
    List<TimeOfUseReportVO> sumTimeOfUseDaily(LocalDate startDate, LocalDate endDate,
                                               Long customerId, Long groupId);

    /**
     * 复费率计费电量日报表明细（按 meter×date 展开，不聚合）。
     * <p>包含商户号/表号/各时段电量和电费，用于明细表格展示。</p>
     */
    List<TimeOfUseReportVO> sumTimeOfUseDailyDetail(LocalDate startDate, LocalDate endDate,
                                                     Long customerId, Long groupId);

    /**
     * 复费率计费电量汇总表（按月聚合尖/峰/平/谷）。
     */
    List<TimeOfUseReportVO> sumTimeOfUseMonthly(LocalDate startDate, LocalDate endDate,
                                                 Long customerId, Long groupId);

    // ──────────────── 计费金额报表（§五.8/9/10） ────────────────

    /**
     * 计费电量汇总表（按日聚合扣费金额）。
     */
    List<BillingReportVO> sumBillingDaily(LocalDate startDate, LocalDate endDate,
                                           Long customerId, Long groupId);

    /**
     * 计费电量月报表（按月聚合扣费金额）。
     */
    List<BillingReportVO> sumBillingMonthly(LocalDate startDate, LocalDate endDate,
                                             Long customerId, Long groupId);

    /**
     * 计费电量年报表（按年聚合扣费金额）。
     */
    List<BillingReportVO> sumBillingYearly(LocalDate startDate, LocalDate endDate,
                                            Long customerId, Long groupId);

    // ──────────────── 用电负荷查询（§五.12） ────────────────

    /**
     * 按日查询用电负荷（最大/平均/最小日用电量 + 负荷率）。
     * <p>对每日各表计总用电量做 max/avg/min 聚合，反映每日峰值负荷和负荷均衡程度。</p>
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<ElectricLoadVO> getDailyElectricLoad(LocalDate startDate, LocalDate endDate,
                                               Long customerId, Long groupId);

    /**
     * 按月查询用电负荷（最大/平均/最小日用电量 + 负荷率）。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<ElectricLoadVO> getMonthlyElectricLoad(LocalDate startDate, LocalDate endDate,
                                                 Long customerId, Long groupId);

    // ──────────────── 用电负荷曲线（§五.12 曲线图） ────────────────

    /**
     * 查询用电负荷曲线数据（按日聚合总用电量 + 分时用电量）。
     * <p>专用于前端 ECharts 曲线图渲染，按日期升序返回。
     * 每条记录包含当日总用电量及尖/峰/平/谷时段分量。</p>
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<ElectricLoadCurveVO> getElectricLoadCurve(LocalDate startDate, LocalDate endDate,
                                                    Long customerId, Long groupId);

    // ──────────────── 用电量月报表明细（按表计+每日用量） ────────────────

    /**
     * 查询某月内各表计每日用电量明细。
     * <p>
     * 按表计维度展开每日用量，包含设备类型、分组、商户、虚拟表等维度信息。
     * 前端据此动态生成日列（如 6/01、6/02 … 6/11）和总用量列。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<MonthlyMeterDetailVO> listMonthlyMeterDetail(LocalDate startDate, LocalDate endDate,
                                                       Long customerId, Long groupId);

    // ──────────────── 月度用电量汇总明细（按表计×月度，含正向/反向/余额） ────────────────

    /**
     * 查询月度用电量汇总明细（按表计×月度维度）。
     * <p>
     * 数据来源：tb_meter_monthly_report 预聚合月报表，
     * 包含正向/反向有功电量起止示数、总起止示数、
     * 上期/本期余额及数据更新时间等完整字段。
     *
     * @param statMonth 统计月份（yyyy-MM）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<MonthlyElecSummaryVO> listMonthlyElecSummary(String statMonth, Long customerId, Long groupId);

    // ──────────────── 已删除表计月报（§五.3） ────────────────

    /**
     * 查询已删除电表的月度用电汇总。
     *
     * @param statMonth  统计月份（yyyy-MM）
     * @param merchantId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<DeletedMeterReportVO> getDeletedMeterMonthlyReport(String statMonth, Long merchantId, Long groupId);

    // ──────────────── 年度用电量汇总（按表计×12月明细） ────────────────

    /**
     * 查询指定年份内各表计的月度用电量明细（1月~12月分列 + 小计）。
     *
     * @param year       统计年份（yyyy）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<YearlyMeterDetailVO> getYearlyMeterDetail(String year, Long customerId, Long groupId);

    // ──────────────── 计费电量年报明细（按表计+年度/本月汇总宽表） ────────────────

    /**
     * 查询计费电量年报明细（按表计+年度/本月汇总宽表）。
     * <p>
     * 每个表计一行：当前年度计费金额/用电量合计 + 当前月度计费金额/用电量合计 +
     * 当月余额信息。虚拟表信息由 Service 层批量补充。
     *
     * @param year       统计年份（yyyy，如 "2026"）
     * @param statMonth  统计月份（yyyy-MM，如 "2026-06"）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<BillingYearlyMeterDetailVO> getBillingYearlyMeterDetail(String year, String statMonth,
                                                                   Long customerId, Long groupId);

    // ──────────────── 计费电量汇总（按表计维度） ────────────────

    /**
     * 按表计维度聚合指定日期范围内的计费金额与用电量。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<BillingMeterSummaryVO> getBillingMeterSummary(LocalDate startDate, LocalDate endDate,
                                                       Long customerId, Long groupId);

    // ──────────────── 计费电量月报表明细（按表计+每日计费） ────────────────

    /**
     * 查询当月各表计每日计费电量与金额明细。
     * <p>
     * 按表计维度展开每日计费数据（electricity_consumed + charge_amount），
     * 包含设备类型、分组、商户、虚拟表等维度信息。
     * 前端据此动态生成日列（二级表头：日期→总电量/总费用）和汇总列。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     */
    List<BillingMonthlyMeterVO> getBillingMonthlyMeterDetail(LocalDate startDate, LocalDate endDate,
                                                              Long customerId, Long groupId);
}
