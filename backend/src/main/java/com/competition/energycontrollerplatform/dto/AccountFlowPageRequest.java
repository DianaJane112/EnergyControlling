package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分账明细（资金流水）分页查询请求。
 * <p>支持按时段、按商户、按交易类型过滤。
 */
@Data
@Schema(description = "分账明细分页查询请求")
public class AccountFlowPageRequest {

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

    @Schema(description = "交易类型：1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零,7-公共分摊")
    private Integer tradeType;

    @Schema(description = "关键字搜索（流水号/业务单号）")
    private String keyword;
}
