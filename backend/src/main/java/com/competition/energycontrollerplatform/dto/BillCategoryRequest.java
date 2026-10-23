package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账单分类请求 DTO（新增/编辑共用）。
 */
@Data
@Schema(description = "账单分类请求")
public class BillCategoryRequest {

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "物业费")
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @Schema(description = "分类编码（唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROPERTY_FEE")
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @Schema(description = "分类描述", example = "物业管理相关费用")
    private String description;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-停用，1-启用", example = "1")
    private Integer status;
}
