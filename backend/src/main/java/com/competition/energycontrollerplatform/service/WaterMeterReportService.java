package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 水表用水量报表服务接口。
 * <p>
 * 提供按天/按月聚合的水表用水量查询能力。
 * 数据来源：水表主表 + 水表读数流水表（原始读数实时聚合）。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
public interface WaterMeterReportService {

    // ──────────────── 日报表 ────────────────

    /**
     * 用水量日报表明细（按「天 + 水表」维度分页）。
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param startDate    开始日期（可选）
     * @param endDate      结束日期（可选）
     * @param merchantId   商户ID（可选，非 ADMIN 强制覆盖）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 分页结果，含 list（当前页数据）与 total（总记录数）
     */
    PageResultVO<WaterDailyUsageVO> listDailyDetail(int pageNum, int pageSize,
                                                     LocalDate startDate, LocalDate endDate,
                                                     Long merchantId, String merchantName, String deviceNo);

    /**
     * 用水量日报表汇总（按「天」聚合，不区分设备）。
     */
    List<WaterDailyUsageVO> sumDaily(LocalDate startDate, LocalDate endDate,
                                     Long merchantId, String deviceNo);

    // ──────────────── 月报表 ────────────────

    /**
     * 用水量月报表明细（按「月 + 水表」维度分页）。
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param statMonth    统计月份 YYYY-MM（可选）
     * @param statYear     统计年份 YYYY（可选）
     * @param merchantId   商户ID（可选）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 分页结果，含 list（当前页数据）与 total（总记录数）
     */
    PageResultVO<WaterMonthlyUsageVO> listMonthlyDetail(int pageNum, int pageSize,
                                                         String statMonth, String statYear,
                                                         Long merchantId, String merchantName, String deviceNo);

    /**
     * 用水量月报表汇总（按「月」聚合，不区分设备）。
     */
    List<WaterMonthlyUsageVO> sumMonthly(String statMonth, Long merchantId, String deviceNo);

    // ──────────────── 已删除水表月报表 ────────────────

    /**
     * 已删除水表月度用水量查询。
     * <p>
     * <b>业务背景：</b>财务对总账时需要统计已被逻辑删除（is_deleted=1）的水表
     * 在历史月份中产生的用水量。本方法绕过 MyBatis-Plus 的 {@code @TableLogic} 插件，
     * 直接在自定义 XML SQL 中写 {@code wm.is_deleted = 1} 条件。
     * </p>
     *
     * @param statMonth    统计月份 YYYY-MM（可选，不传则查所有月份）
     * @param merchantId   商户ID（可选，非 ADMIN 强制覆盖）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 已删除水表按月聚合的用水量列表
     */
    List<WaterDeletedMeterMonthlyVO> getDeletedMeterMonthlyReport(String statMonth,
                                                                    Long merchantId,
                                                                    String merchantName,
                                                                    String deviceNo);

    // ──────────────── 全园区月度/年度汇总（管理层视角） ────────────────

    /**
     * 全园区月度用水量汇总。
     * <p>
     * <b>管理层视角：</b>不区分水表/商户，按自然月聚合全园区总用水量。
     * 返回数据可直接用于前端折线图/柱状图渲染（X轴=月份，Y轴=用水量）。
     * </p>
     *
     * @param statYear   统计年份 YYYY（可选，如 "2026"，只查该年各月；不传则查全部）
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，非 ADMIN 强制覆盖；传 null = 全园区）
     * @return 按月份升序排列的园区总用水量列表
     */
    List<WaterMonthlySummaryVO> getCampusMonthlySummary(String statYear,
                                                         LocalDate startDate, LocalDate endDate,
                                                         Long merchantId);

    /**
     * 全园区年度用水量汇总。
     * <p>
     * <b>管理层视角：</b>不区分水表/商户，按自然年聚合全园区总用水量。
     * 返回数据可直接用于前端年度对比柱状图（X轴=年份，Y轴=用水量）。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，非 ADMIN 强制覆盖；传 null = 全园区）
     * @return 按年份升序排列的园区总用水量列表
     */
    List<WaterYearlySummaryVO> getCampusYearlySummary(LocalDate startDate, LocalDate endDate,
                                                       Long merchantId);

    // ──────────────── 计费水量报表（财务视角，含平均单价） ────────────────

    /**
     * 计费水量汇总表（按天聚合）。
     * <p>
     * <b>财务视角：</b>同时统计用水量与水费，Service 层自动计算平均单价
     * （totalFee ÷ totalUsage），供财务核费和对账使用。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，非 ADMIN 强制覆盖）
     * @return 按日期升序排列的计费汇总列表，含平均单价
     */
    List<WaterBillingSummaryVO> getBillingDailySummary(LocalDate startDate, LocalDate endDate,
                                                        Long merchantId);

    /**
     * 计费水量月报表（按月聚合）。
     * <p>
     * <b>财务视角：</b>按月统计用水量与水费，自动计算月平均单价，
     * 支持环比/同比分析。
     * </p>
     *
     * @param statMonth  统计月份 YYYY-MM（可选）
     * @param statYear   统计年份 YYYY（可选）
     * @param merchantId 商户ID（可选，非 ADMIN 强制覆盖）
     * @return 按月份升序排列的计费月报列表，含平均单价
     */
    List<WaterBillingMonthlyVO> getBillingMonthlySummary(String statMonth, String statYear,
                                                          Long merchantId);

    /**
     * 计费水量年报表（按年聚合）。
     * <p>
     * <b>财务视角：</b>按年统计用水量与水费，自动计算年平均单价，
     * 适合年度财务决算和水费预算编制。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，非 ADMIN 强制覆盖）
     * @return 按年份升序排列的计费年报列表，含平均单价
     */
    List<WaterBillingYearlyVO> getBillingYearlySummary(LocalDate startDate, LocalDate endDate,
                                                        Long merchantId);
}
