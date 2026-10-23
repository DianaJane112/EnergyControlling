package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新增/编辑账单请求 DTO。
 */
@Data
@Schema(description = "新增/编辑账单请求")
public class BillCreateRequest {

    @Schema(description = "账单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账单名称不能为空")
    private String billName;

    @Schema(description = "账单分类ID")
    private Long categoryId;

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号")
    private String meterNo;

    @Schema(description = "虚拟表计ID")
    private Long virtualMeterId;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "商户名")
    private String merchantName;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "收款金额")
    private BigDecimal receivedAmount;

    @Schema(description = "待收金额")
    private BigDecimal pendingAmount;

    @Schema(description = "支付状态", example = "未支付")
    private String paymentStatus;

    @Schema(description = "欠费处理方式", example = "跳闸")
    private String arrearsHandling;

    @Schema(description = "是否支持线上支付", example = "1")
    private Integer supportOnlinePayment;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "账单生成时间")
    private LocalDateTime billCreateTime;

    @Schema(description = "最后支付时间")
    private LocalDateTime lastPaymentTime;

    @Schema(description = "实际支付时间")
    private LocalDateTime actualPaymentTime;

    @Schema(description = "备注")
    private String remark;
}
