package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 复费率分时用电量响应 VO（§五.4/5）。
 * <p>包含尖/峰/平/谷四个时段的用电量与电费分解，支撑分时图表渲染和明细表格。</p>
 */
@Data
@Schema(description = "复费率分时用电量条目")
public class TimeOfUseReportVO {

    @Schema(description = "报表日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @Schema(description = "统计时段（日/月/年）")
    private String period;

    @Schema(description = "商户号（customer_code）")
    private String customerCode;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "表号（设备号/钢印号）")
    private String deviceNo;

    @Schema(description = "表计ID")
    private Long meterId;

    // ── 总用量 ──

    @Schema(description = "总用电量（kWh）")
    private BigDecimal totalConsumed;

    @Schema(description = "总电费（元）")
    private BigDecimal totalCharge;

    // ── 尖用量 ──

    @Schema(description = "尖时段用电量（kWh）")
    private BigDecimal peakConsumed;

    @Schema(description = "尖时段电费（元）")
    private BigDecimal peakCharge;

    // ── 峰用量 ──

    @Schema(description = "峰时段用电量（kWh）")
    private BigDecimal busyConsumed;

    @Schema(description = "峰时段电费（元）")
    private BigDecimal busyCharge;

    // ── 平用量 ──

    @Schema(description = "平时段用电量（kWh）")
    private BigDecimal normalConsumed;

    @Schema(description = "平时段电费（元）")
    private BigDecimal normalCharge;

    // ── 谷用量 ──

    @Schema(description = "谷时段用电量（kWh）")
    private BigDecimal idleConsumed;

    @Schema(description = "谷时段电费（元）")
    private BigDecimal idleCharge;

    @Schema(description = "涉及表计数（汇总视图使用）")
    private Long meterCount;
}
