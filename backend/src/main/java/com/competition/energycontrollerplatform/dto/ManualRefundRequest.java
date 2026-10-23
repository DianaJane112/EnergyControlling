package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 人工退费请求 DTO。
 * <p>
 * 安全约束：非 ADMIN 角色仅可操作自身关联商户的表计；
 * 退费前校验商户余额 >= 退费金额，使用原子 SQL 扣减。
 */
@Data
@Schema(description = "人工退费请求")
public class ManualRefundRequest {

    @Schema(description = "表计ID（必填，定位退费来源表计）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表计ID不能为空")
    private Long meterId;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制使用当前登录商户）", example = "1")
    private Long customerId;

    @Schema(description = "退费金额（元），必须大于0", example = "200.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退费金额不能为空")
    @DecimalMin(value = "0.01", message = "退费金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "退费原因（必填，用于审计追溯）", example = "商户退租，退还剩余电费余额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退费原因不能为空")
    private String reason;

    @Schema(description = "备注")
    private String remark;
}
