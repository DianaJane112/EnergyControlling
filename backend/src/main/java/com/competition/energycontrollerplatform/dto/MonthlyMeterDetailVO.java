package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用电量月报表明细 VO。
 * <p>
 * 按表计维度展示某月内每日用电量明细，支持前端动态生成日列。
 * 包含设备类型、分组、商户、虚拟表等维度信息。
 */
@Data
@Schema(description = "用电量月报表明细条目（按表计+每日用量）")
public class MonthlyMeterDetailVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号（设备号）", example = "CM2024001")
    private String deviceNo;

    @Schema(description = "设备类型", example = "ELECTRIC")
    private String meterType;

    @Schema(description = "分组名称", example = "B13栋")
    private String groupName;

    @Schema(description = "商户名称", example = "某某科技有限公司")
    private String customerName;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "每日用量 Map，key=MM-dd（如 06-01），value=当日用电量(kWh)")
    private Map<String, BigDecimal> dailyUsage;

    @Schema(description = "月总用量（kWh）")
    private BigDecimal totalUsage;
}
