package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 余额调整分页查询请求。
 * <p>支持按时段、商户ID、调整类型、关键字过滤。
 * 非 ADMIN 角色强制隔离为当前登录商户。
 */
@Data
@Schema(description = "余额调整分页查询请求")
public class BalanceAdjustmentPageRequest {

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

    @Schema(description = "调整类型：INCREASE-增加, DECREASE-扣减", example = "INCREASE")
    private String adjustmentType;

    @Schema(description = "关键字搜索（调整编号/操作员/原因/表号/商户名）")
    private String keyword;

    @Schema(description = "分组名称（精确匹配）")
    private String groupName;
}
