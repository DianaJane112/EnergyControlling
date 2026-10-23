package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 月用电趋势数据点。
 * <p>
 * 对应小程序"用能分析 → 月用电趋势"柱状图/折线图的一个数据点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "月用电趋势数据点")
public class MonthlyTrendVO {

    @Schema(description = "月份（yyyy-MM）", example = "2026-06")
    private String month;

    @Schema(description = "当月用电量（kWh）", example = "3850.00")
    private BigDecimal usage;
}
