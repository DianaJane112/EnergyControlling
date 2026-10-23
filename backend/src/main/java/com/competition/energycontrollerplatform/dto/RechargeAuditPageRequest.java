package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端 — 充值审核分页请求
 */
@Data
@Schema(description = "充值审核分页请求")
public class RechargeAuditPageRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "审核状态: PENDING=待审核, APPROVED=已通过, REJECTED=已拒绝，不传=全部")
    private String auditStatus;

    @Schema(description = "搜索关键词（商户名称/表号/申请编号）")
    private String keyword;
}
