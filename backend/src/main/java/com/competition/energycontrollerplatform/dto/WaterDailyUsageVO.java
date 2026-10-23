package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 水表用水量日报表响应 VO。
 * <p>
 * 按天聚合每台水表的用水量与费用。
 * 数据来源：t_water_meter_reading（水表读数流水表）+ t_water_meter（水表主表）。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "水表用水量日报表条目")
public class WaterDailyUsageVO {

    @Schema(description = "统计日期（yyyy-MM-dd）", example = "2026-06-07")
    private LocalDate statDate;

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

    @Schema(description = "日用水量（吨）", example = "12.50")
    private BigDecimal totalUsage;

    @Schema(description = "日费用（元）", example = "42.50")
    private BigDecimal totalFee;

    @Schema(description = "当日有读数记录的水表数（仅汇总接口返回）", example = "48")
    private Integer meterCount;
}
