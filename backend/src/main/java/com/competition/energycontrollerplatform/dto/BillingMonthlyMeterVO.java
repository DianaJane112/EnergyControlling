package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 计费电量月报表（按表计+每日计费明细）VO。
 * <p>
 * 按表计维度展示当月每日的计费电量与计费金额，
 * 前端据此动态生成日列（二级表头：日期→总电量/总费用）与汇总列。
 * <p>
 * 日期范围从当月1日到当前日期，跨月自动清零重置。
 */
@Data
@Schema(description = "计费电量月报表明细条目（按表计+每日计费）")
public class BillingMonthlyMeterVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "设备类型", example = "租户/总表/公共/其他")
    private String deviceType;

    @Schema(description = "分组名称", example = "B13栋")
    private String groupName;

    @Schema(description = "商户名称", example = "某某科技有限公司")
    private String merchantName;

    @Schema(description = "表号（设备号）", example = "CM2024001")
    private String deviceNo;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "每日计费电量 Map，key=MM-dd（如 06-01），value=当日计费电量(kWh)")
    private Map<String, BigDecimal> dailyKwh;

    @Schema(description = "每日计费金额 Map，key=MM-dd（如 06-01），value=当日计费金额(元)")
    private Map<String, BigDecimal> dailyAmt;

    @Schema(description = "月总计费电量（kWh）")
    private BigDecimal totalKwh;

    @Schema(description = "月总计费金额（元）")
    private BigDecimal totalAmt;
}
