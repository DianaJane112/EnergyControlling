package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 人工缴费（充值）请求 DTO。
 * <p>
 * 注意：非 ADMIN 角色前端传入的 customerId/meterId 会被后端强制覆盖为当前登录用户的关联商户。
 */
@Data
@Schema(description = "人工缴费请求")
public class DepositRequest {

    @Schema(description = "表计ID（可选，绑定到具体表计）", example = "1")
    private Long meterId;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制使用当前登录商户）", example = "1")
    private Long customerId;

    @Schema(description = "缴费金额（元），必须大于0", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缴费金额不能为空")
    @DecimalMin(value = "0.01", message = "缴费金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "缴费方式：CASH-现金, TRANSFER-转账, WECHAT-微信, ALIPAY-支付宝", example = "TRANSFER")
    private String paymentMethod;

    @Schema(description = "备注", example = "2026年6月电费预缴")
    private String remark;
}
