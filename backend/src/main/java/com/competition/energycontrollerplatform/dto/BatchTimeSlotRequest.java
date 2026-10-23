package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量/全局设置时段策略请求 DTO。
 * <p>
 * 将指定的费率时段策略批量分配到表计，支持全局模式。
 */
@Data
@Schema(description = "批量设置时段策略请求")
public class BatchTimeSlotRequest {

    @Schema(description = "表计ID列表（为空+isGlobal=true表示全局设置）")
    private List<Long> meterIds;

    @Schema(description = "是否全局设置所有表计（仅ADMIN）", example = "false")
    private Boolean isGlobal;

    @Schema(description = "按商户ID批量设置（可选）")
    private Long customerId;

    @Schema(description = "表计大类筛选：0=仅水表, 1=仅电表, null=全部（全局模式下用于限定表计类型）", example = "1")
    private Integer meterType;

    @Schema(description = "费率策略ID（关联t_price_strategy.id）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "费率策略ID不能为空")
    private Long rateConfigId;
}
