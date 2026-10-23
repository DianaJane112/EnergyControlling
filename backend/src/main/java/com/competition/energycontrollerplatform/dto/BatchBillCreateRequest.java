package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量创建账单请求。
 */
@Data
@Schema(description = "批量创建账单请求")
public class BatchBillCreateRequest {

    @Schema(description = "表计ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "表计ID列表不能为空")
    private List<Long> meterIds;

    @Schema(description = "账单分类ID")
    private Long categoryId;

    @Schema(description = "账单说明")
    private String billDescription;

    @Schema(description = "固定金额（元，为空则按实时用量计算）")
    private BigDecimal fixedAmount;

    @Schema(description = "操作人", example = "admin")
    private String operatorName;

    @Schema(description = "备注")
    private String remark;
}
