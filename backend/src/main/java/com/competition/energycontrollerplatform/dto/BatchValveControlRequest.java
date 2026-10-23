package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 批量/全局开关阀控制请求 DTO。
 * <p>
 * 支持批量操作指定表计或全局所有表计的开关阀（分合闸）。
 */
@Data
@Schema(description = "批量/全局开关阀请求")
public class BatchValveControlRequest {

    @Schema(description = "表计ID列表（为空+isGlobal=true表示全局操作）")
    private List<Long> meterIds;

    @Schema(description = "是否全局操作所有表计（仅ADMIN）", example = "false")
    private Boolean isGlobal;

    @Schema(description = "按商户ID批量操作（可选）")
    private Long customerId;

    @Schema(description = "表计大类筛选：0=仅水表, 1=仅电表, null=全部（全局模式下用于限定表计类型）", example = "1")
    private Integer meterType;

    @Schema(description = "操作类型：OPEN-开阀(合闸), CLOSE-关阀(分闸)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String action; // OPEN or CLOSE

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作原因不能为空")
    private String reason;
}
