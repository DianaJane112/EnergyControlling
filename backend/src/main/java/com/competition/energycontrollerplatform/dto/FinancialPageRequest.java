package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 财务管理分页查询请求（充值明细/退费明细共用基类）。
 * <p>非 ADMIN 角色前端传入的 customerId 会被强制覆盖为当前登录用户关联商户。
 */
@Data
@Schema(description = "财务明细分页查询请求")
public class FinancialPageRequest {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "20", defaultValue = "20")
    private Integer pageSize;

    @Schema(description = "开始时间（含）", example = "2026-05-01 00:00:00")
    private String startTime;

    @Schema(description = "结束时间（含）", example = "2026-05-31 23:59:59")
    private String endTime;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制覆盖）")
    private Long customerId;

    @Schema(description = "关键字搜索（订单号/商户名）")
    private String keyword;

    @Schema(description = "交易类型：1-充值电费,2-充值水费")
    private Integer tradeType;
}
