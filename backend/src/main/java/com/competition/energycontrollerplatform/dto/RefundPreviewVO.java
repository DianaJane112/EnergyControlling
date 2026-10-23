package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退费预览响应 VO。
 * <p>在正式退费前展示：表计信息、商户信息、当前余额、退费后余额、
 * 当前电价等信息，供操作员确认。
 */
@Data
@Schema(description = "退费预览信息")
public class RefundPreviewVO {

    // ── 表计信息 ──
    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号")
    private String dbNo;

    @Schema(description = "表计地址")
    private String meterAddress;

    @Schema(description = "表计类型（1-电表,2-水表,3-燃气表）")
    private Integer meterType;

    @Schema(description = "表计类型名称")
    private String meterTypeName;

    @Schema(description = "表计状态（1-运行,2-欠费停机,3-拆表销户）")
    private Integer meterStatus;

    @Schema(description = "表计状态名称")
    private String meterStatusName;

    // ── 商户信息 ──
    @Schema(description = "商户ID")
    private Long customerId;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "商户联系电话")
    private String customerPhone;

    // ── 余额信息 ──
    @Schema(description = "当前余额（元）")
    private BigDecimal currentBalance;

    @Schema(description = "警告额度（元）")
    private BigDecimal warnline;

    @Schema(description = "透支额度（元）")
    private BigDecimal credit;

    // ── 电价信息 ──
    @Schema(description = "当前电价（元/kWh）")
    private BigDecimal currentPrice;

    @Schema(description = "电价策略名称")
    private String priceStrategyName;

    // ── 退费预览 ──
    @Schema(description = "拟退费金额（元）")
    private BigDecimal refundAmount;

    @Schema(description = "退费后余额（元）")
    private BigDecimal balanceAfterRefund;

    @Schema(description = "退费后是否触发欠费停机")
    private Boolean willTriggerShutdown;

    @Schema(description = "预览时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime previewTime;
}
