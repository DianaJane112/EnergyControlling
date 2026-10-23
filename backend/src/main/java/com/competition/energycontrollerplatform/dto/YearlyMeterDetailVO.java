package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 年度用电量汇总表（按表计×12月明细）VO。
 * <p>
 * 前端列：序号 | 年度 | 商户名 | 表号 | 虚拟表号 | 虚拟表商户名 | 表计类型 |
 * 1月 | 2月 | … | 12月 | 小计
 */
@Data
@Schema(description = "年度用电量汇总条目（按表计+12月明细）")
public class YearlyMeterDetailVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "统计年份", example = "2026")
    private String year;

    @Schema(description = "商户名称", example = "天辰超市")
    private String customerName;

    @Schema(description = "表号/设备号", example = "CM2024001")
    private String deviceNo;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "表计类型：租户/总表/公共/其他", example = "租户")
    private String meterType;

    // ── 12个月 ──

    @Schema(description = "1月用电量(kWh)")
    private BigDecimal month01 = BigDecimal.ZERO;

    @Schema(description = "2月用电量(kWh)")
    private BigDecimal month02 = BigDecimal.ZERO;

    @Schema(description = "3月用电量(kWh)")
    private BigDecimal month03 = BigDecimal.ZERO;

    @Schema(description = "4月用电量(kWh)")
    private BigDecimal month04 = BigDecimal.ZERO;

    @Schema(description = "5月用电量(kWh)")
    private BigDecimal month05 = BigDecimal.ZERO;

    @Schema(description = "6月用电量(kWh)")
    private BigDecimal month06 = BigDecimal.ZERO;

    @Schema(description = "7月用电量(kWh)")
    private BigDecimal month07 = BigDecimal.ZERO;

    @Schema(description = "8月用电量(kWh)")
    private BigDecimal month08 = BigDecimal.ZERO;

    @Schema(description = "9月用电量(kWh)")
    private BigDecimal month09 = BigDecimal.ZERO;

    @Schema(description = "10月用电量(kWh)")
    private BigDecimal month10 = BigDecimal.ZERO;

    @Schema(description = "11月用电量(kWh)")
    private BigDecimal month11 = BigDecimal.ZERO;

    @Schema(description = "12月用电量(kWh)")
    private BigDecimal month12 = BigDecimal.ZERO;

    @Schema(description = "年度小计(kWh) = 1~12月合计")
    private BigDecimal subtotal = BigDecimal.ZERO;
}
