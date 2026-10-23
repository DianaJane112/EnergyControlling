package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量延期请求。
 */
@Data
@Schema(description = "批量延期请求")
public class BatchDeferRequest {

    @Schema(description = "表计ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "表计ID列表不能为空")
    private List<Long> meterIds;

    @Schema(description = "延期至时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-01 00:00:00")
    private LocalDateTime deferTo;

    @Schema(description = "延期原因", example = "商户申请延期缴费")
    private String deferReason;

    @Schema(description = "操作人", example = "admin")
    private String operatorName;
}
