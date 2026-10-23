package com.competition.energycontrollerplatform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 水表报表 Mapper。
 * <p>
 * 全部 SQL 定义在 resources/mapper/WaterMeterReportMapper.xml 中，
 * 这里只声明 Java 接口方法，方法名与 XML 中的 &lt;select&gt; id 一一对应。
 * </p>
 * <p>
 * 数据来源：t_meter (meter_type=0 水表) + t_meter_reading (读数 kwh) + t_customer (商户名)。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Mapper
public interface WaterMeterReportMapper {

    /**
     * 用水量日报表明细：按「天 + 水表」维度聚合。
     * <p>支持按日期范围、商户ID、商户名模糊搜索、设备号筛选。</p>
     *
     * @param startDate    开始日期（可选）
     * @param endDate      结束日期（可选）
     * @param merchantId   商户ID（可选）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 每行包含 statDate / meterId / merchantId / merchantName / deviceNo / totalUsage / totalFee
     */
    List<Map<String, Object>> selectDailyDetail(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("merchantId") Long merchantId,
                                                @Param("merchantName") String merchantName,
                                                @Param("deviceNo") String deviceNo);

    /**
     * 用水量日报表汇总：按「天」维度聚合（不区分设备），每日一行汇总数据。
     * <p>支持按日期范围、商户ID、设备号筛选。</p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选）
     * @param deviceNo   设备编号精确搜索（可选）
     * @return 每行包含 statDate / totalUsage / totalFee / meterCount（参与统计的水表数）
     */
    List<Map<String, Object>> sumDaily(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("merchantId") Long merchantId,
                                       @Param("deviceNo") String deviceNo);

    /**
     * 用水量月报表明细：按「月 + 水表」维度聚合。
     * <p>支持按月份、年份、商户ID、商户名模糊搜索、设备号筛选。</p>
     *
     * @param statMonth    统计月份 YYYY-MM（可选）
     * @param statYear     统计年份 YYYY（可选）
     * @param merchantId   商户ID（可选）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 每行包含 statMonth / meterId / merchantId / merchantName / deviceNo / totalUsage / totalFee / dailyCount
     */
    List<Map<String, Object>> selectMonthlyDetail(@Param("statMonth") String statMonth,
                                                   @Param("statYear") String statYear,
                                                   @Param("merchantId") Long merchantId,
                                                   @Param("merchantName") String merchantName,
                                                   @Param("deviceNo") String deviceNo);

    /**
     * 用水量月报表汇总：按「月」维度聚合（不区分设备），每月一行汇总数据。
     * <p>支持按月份、商户ID、设备号筛选。</p>
     *
     * @param statMonth  统计月份 YYYY-MM（可选）
     * @param merchantId 商户ID（可选）
     * @param deviceNo   设备编号精确搜索（可选）
     * @return 每行包含 statMonth / totalUsage / totalFee / meterCount / dailyCount
     */
    List<Map<String, Object>> sumMonthly(@Param("statMonth") String statMonth,
                                         @Param("merchantId") Long merchantId,
                                         @Param("deviceNo") String deviceNo);

    // ──────────────── 已删除水表月报表 ────────────────

    /**
     * 已删除水表月度用水量查询（核心 SQL）。
     * <p>
     * <b>绕过 MyBatis-Plus 逻辑删除：</b>
     * 本方法对应的 XML SQL 显式使用 {@code INNER JOIN t_water_meter wm ON ... AND wm.is_deleted = 1}，
     * 自定义 XML 中的 SQL 不会被 MyBatis-Plus 的 {@code @TableLogic} 拦截器改写，
     * 因此可以直接查询已删除的水表数据。
     * </p>
     * <p>
     * 支持按月份、商户ID、商户名模糊搜索、设备编号筛选。
     * </p>
     *
     * @param statMonth    统计月份 YYYY-MM（可选）
     * @param merchantId   商户ID（可选）
     * @param merchantName 商户名模糊搜索（可选）
     * @param deviceNo     设备编号精确搜索（可选）
     * @return 每行包含 statMonth / meterId / merchantId / merchantName /
     *         deviceNo / totalUsage / totalFee / dailyCount /
     *         deletedTime / dataSource
     */
    List<Map<String, Object>> selectDeletedMeterMonthly(@Param("statMonth") String statMonth,
                                                         @Param("merchantId") Long merchantId,
                                                         @Param("merchantName") String merchantName,
                                                         @Param("deviceNo") String deviceNo);

    // ──────────────── 全园区月度/年度汇总（管理层视角） ────────────────

    /**
     * 全园区月度用水量汇总：按「自然月」聚合所有水表的总用水量。
     * <p>
     * <b>管理层视角：</b>不区分具体水表或商户，只按月份 GROUP BY 得到园区整体趋势，
     * 返回数据结构适合前端直接渲染折线图/柱状图。
     * </p>
     * <p>
     * SQL: {@code GROUP BY DATE_FORMAT(record_time, '%Y-%m')}，可选按年份或日期范围过滤。
     * </p>
     *
     * @param statYear  统计年份 YYYY（可选，如 "2026"，只查该年各月）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param merchantId 商户ID（可选，非ADMIN强制覆盖；传null=全园区）
     * @return 每行包含 statMonth / totalUsage / totalFee / meterCount / dailyCount
     */
    List<Map<String, Object>> sumCampusMonthly(@Param("statYear") String statYear,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("merchantId") Long merchantId);

    /**
     * 全园区年度用水量汇总：按「自然年」聚合所有水表的总用水量。
     * <p>
     * <b>管理层视角：</b>按 {@code YEAR(record_time)} GROUP BY 得到园区年度总量趋势。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，非ADMIN强制覆盖；传null=全园区）
     * @return 每行包含 statYear / totalUsage / totalFee / meterCount / monthCount
     */
    List<Map<String, Object>> sumCampusYearly(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("merchantId") Long merchantId);

    // ──────────────── 计费水量报表（财务视角，含平均单价） ────────────────

    /**
     * 计费水量汇总表：按「天」聚合用水量 + 水费。
     * <p>
     * <b>财务视角：</b>同时 SUM(usage_amount) 和 SUM(fee)，
     * Service 层用 totalFee ÷ totalUsage 算出平均单价供财务核费。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选，传 null = 全园区）
     * @return 每行包含 statDate / totalUsage / totalFee / meterCount
     */
    List<Map<String, Object>> sumBillingDaily(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("merchantId") Long merchantId);

    /**
     * 计费水量月报表：按「月」聚合用水量 + 水费。
     * <p>
     * <b>财务视角：</b>按月 SUM 后计算平均单价，支持环比/同比分析。
     * </p>
     *
     * @param statMonth  统计月份 YYYY-MM（可选）
     * @param statYear   统计年份 YYYY（可选）
     * @param merchantId 商户ID（可选）
     * @return 每行包含 statMonth / totalUsage / totalFee / meterCount / dailyCount
     */
    List<Map<String, Object>> sumBillingMonthly(@Param("statMonth") String statMonth,
                                                 @Param("statYear") String statYear,
                                                 @Param("merchantId") Long merchantId);

    /**
     * 计费水量年报表：按「年」聚合用水量 + 水费。
     * <p>
     * <b>财务视角：</b>按年 SUM 后计算平均单价，适合年度决算。
     * </p>
     *
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param merchantId 商户ID（可选）
     * @return 每行包含 statYear / totalUsage / totalFee / meterCount / monthCount
     */
    List<Map<String, Object>> sumBillingYearly(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("merchantId") Long merchantId);
}
