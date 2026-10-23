package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 小程序仪表盘响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小程序仪表盘响应")
public class DashboardVO {

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "当前电费余额（元）")
    private BigDecimal electricBalance;

    @Schema(description = "电费预计可用天数")
    private Integer electricEstimatedDays;

    @Schema(description = "当前水费余额（元）")
    private BigDecimal waterBalance;

    @Schema(description = "水费预计可用天数")
    private Integer waterEstimatedDays;

    @Schema(description = "今日用电量（kWh）")
    private BigDecimal todayElectricUsage;

    @Schema(description = "近30日用电量（kWh）")
    private BigDecimal last30DaysElectricUsage;

    @Schema(description = "昨日用水量（m³）")
    private BigDecimal yesterdayWaterUsage;

    @Schema(description = "上月用水量（m³）")
    private BigDecimal lastMonthWaterUsage;
}
