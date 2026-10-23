package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公共分摊执行请求 DTO。
 * <p>
 * 用于发起一次公共区域费用分摊操作。
 * 支持两种分摊策略：按户等分、按历史用量比例分摊。
 */
@Data
@Schema(description = "公共分摊执行请求")
public class PublicAllocationExecuteRequest {

    @Schema(description = "项目ID", example = "1")
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "总表ID（公共区域表计）", example = "10")
    private Long sourceMeterId;

    @Schema(description = "总表表号（与 sourceMeterId 二选一，优先使用此字段自动查询表计ID）", example = "ELEC001")
    private String sourceMeterNo;

    @Schema(description = "分摊名称", example = "2026年5月公区分摊")
    private String allocationName;

    @Schema(description = "分摊方式：PER_HOUSEHOLD-按户等分, BY_USAGE_RATIO-按历史用量比例分摊", example = "PER_HOUSEHOLD")
    @NotNull(message = "分摊策略不能为空")
    private String allocationType;

    @Schema(description = "待分摊总金额（元）", example = "500.00")
    @NotNull(message = "分摊总金额不能为空")
    @DecimalMin(value = "0.01", message = "分摊金额必须大于0")
    private BigDecimal totalAmount;

    @Schema(description = "目标电表ID列表（参与分摊的电表）", example = "[1, 2, 3]")
    @NotEmpty(message = "目标电表列表不能为空")
    private List<Long> targetMeterIds;

    @Schema(description = "生效日期", example = "2026-06-01")
    private String effectiveDate;

    @Schema(description = "备注")
    private String remark;
}
