package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 日用电趋势数据点。
 * <p>
 * 对应小程序"用能分析 → 日用电趋势"折线图的一个数据点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "日用电趋势数据点")
public class DailyTrendVO {

    @Schema(description = "日期（yyyy-MM-dd）", example = "2026-06-15")
    private String date;

    @Schema(description = "当日用电量（kWh）", example = "125.50")
    private BigDecimal usage;
}
