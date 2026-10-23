package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端 — 充值审核操作请求（通过/拒绝）
 */
@Data
@Schema(description = "充值审核请求")
public class RechargeAuditRequest {

    @Schema(description = "审核备注。通过时可选，拒绝时必填原因", example = "审核通过，已充值到账")
    @NotBlank(message = "审核备注不能为空")
    private String auditRemark;
}
