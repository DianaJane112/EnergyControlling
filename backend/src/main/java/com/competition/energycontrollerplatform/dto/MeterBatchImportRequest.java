package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 表计批量导入请求
 */
@Data
@Schema(description = "表计批量导入请求")
public class MeterBatchImportRequest {

    @Schema(description = "表计列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "导入列表不能为空")
    @Size(max = 500, message = "单次导入最多500条")
    @Valid
    private List<MeterRequest> meters;
}
