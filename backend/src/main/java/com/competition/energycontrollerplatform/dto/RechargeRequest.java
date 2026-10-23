package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值请求
 */
@Data
@Schema(description = "充值请求")
public class RechargeRequest {

    @Schema(description = "商户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商户ID不能为空")
    private Long customerId;

    @Schema(description = "充值金额（元），必须大于0", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "操作人", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作人不能为空")
    private String operatorName;

    @Schema(description = "用户名/缴费人", example = "张三")
    private String userName;

    @Schema(description = "交易类型：1-充值电费,2-充值水费", example = "1")
    private Integer tradeType;

    @Schema(description = "备注", example = "线下转账充值")
    private String remark;
}
