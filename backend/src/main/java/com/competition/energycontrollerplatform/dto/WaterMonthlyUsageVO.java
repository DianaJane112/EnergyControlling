package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 水表用水量月报表响应 VO。
 * <p>
 * 按自然月聚合每台水表的用水量与费用。
 * 数据来源：t_water_meter_reading（水表读数流水表）+ t_water_meter（水表主表）。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "水表用水量月报表条目")
public class WaterMonthlyUsageVO {

    @Schema(description = "统计月份（yyyy-MM）", example = "2026-06")
    private String statMonth;

    @Schema(description = "水表ID", example = "1001")
    private Long meterId;

    @Schema(description = "商户ID", example = "2001")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "设备编号")
    private String deviceNo;

    @Schema(description = "虚拟表号（来自t_water_meter.virtual_meter_no）")
    private String virtualMeterNo;

    @Schema(description = "虚拟表商户名（来自t_water_meter.virtual_merchant_name）")
    private String virtualMerchantName;

    @Schema(description = "月用水量（吨）", example = "385.20")
    private BigDecimal totalUsage;

    @Schema(description = "月费用（元）", example = "1309.68")
    private BigDecimal totalFee;

    @Schema(description = "月内有读数记录的天数", example = "30")
    private Integer dailyCount;

    @Schema(description = "当月有读数记录的水表数（仅汇总接口返回）", example = "48")
    private Integer meterCount;
}
