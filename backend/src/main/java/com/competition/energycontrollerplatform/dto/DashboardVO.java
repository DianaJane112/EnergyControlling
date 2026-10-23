package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页仪表盘聚合数据响应体
 */
@Data
@Schema(description = "仪表盘聚合数据")
public class DashboardVO {

    @Schema(description = "实时资产大盘")
    private AssetOverview assetOverview;

    @Schema(description = "财务资金大盘")
    private FinanceOverview financeOverview;

    @Schema(description = "过去7天能耗趋势（每日总电耗 + 总水耗）")
    private List<DailyTrend> dailyTrends;

    @Schema(description = "今日24小时分时趋势（每小时总电耗 + 总水耗），缺失小时补0")
    private List<HourlyTrend> todayHourlyTrends;

    @Schema(description = "当月每日趋势（1号~今天/月底，每日总电耗 + 总水耗），缺失日期补0")
    private List<MonthlyDailyTrend> monthlyDailyTrends;

    @Schema(description = "风险预警雷达（余额低于阈值的前5个商户）")
    private List<RiskAlert> riskAlerts;

    // ──── 嵌套视图 ────

    @Data
    @Schema(description = "资产大盘统计")
    public static class AssetOverview {
        @Schema(description = "总设备数", example = "128")
        private Long totalMeters;

        @Schema(description = "激活数（status=1）", example = "110")
        private Long activeMeters;

        @Schema(description = "欠费停机数（status=2）", example = "8")
        private Long overdueMeters;

        @Schema(description = "在线率（%）", example = "85.94")
        private BigDecimal onlineRate;
    }

    @Data
    @Schema(description = "财务资金大盘")
    public static class FinanceOverview {
        @Schema(description = "全盘商户总资金池余额（元）", example = "152300.50")
        private BigDecimal totalBalance;

        @Schema(description = "今日累计充值总额（元）", example = "5200.00")
        private BigDecimal todayRechargeTotal;

        @Schema(description = "今日累计能耗扣费总额（元）", example = "1340.82")
        private BigDecimal todayDebitTotal;
    }

    @Data
    @Schema(description = "单日能耗趋势点")
    public static class DailyTrend {
        @Schema(description = "日期", example = "2026-05-25")
        private String date;

        @Schema(description = "当日总电量消耗（kWh）", example = "1250.50")
        private BigDecimal totalElectricity;

        @Schema(description = "当日总水量消耗（吨）", example = "340.20")
        private BigDecimal totalWater;
    }

    @Data
    @Schema(description = "单小时能耗趋势点")
    public static class HourlyTrend {
        @Schema(description = "小时 (0–23)", example = "14")
        private Integer hour;

        @Schema(description = "该小时总电耗（kWh）", example = "52.30")
        private BigDecimal totalElectricity;

        @Schema(description = "该小时总水耗（吨）", example = "12.50")
        private BigDecimal totalWater;
    }

    @Data
    @Schema(description = "当月单日能耗趋势点")
    public static class MonthlyDailyTrend {
        @Schema(description = "日期", example = "2026-06-15")
        private String date;

        @Schema(description = "当日总电耗（kWh）", example = "1250.50")
        private BigDecimal totalElectricity;

        @Schema(description = "当日总水耗（吨）", example = "340.20")
        private BigDecimal totalWater;
    }

    @Data
    @Schema(description = "风险预警商户")
    public static class RiskAlert {
        @Schema(description = "商户ID", example = "5")
        private Long customerId;

        @Schema(description = "商户名称", example = "电子科大3号商铺")
        private String customerName;

        @Schema(description = "当前余额（元）", example = "23.50")
        private BigDecimal balance;
    }
}
