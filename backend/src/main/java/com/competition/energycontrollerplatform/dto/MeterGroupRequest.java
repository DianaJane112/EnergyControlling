package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 分组管理请求 DTO。
 * <p>
 * 用于创建/修改分组。
 * merchantId 由后端从安全上下文注入，前端不可信。
 */
@Data
@Schema(description = "分组管理请求")
public class MeterGroupRequest {

    @Schema(description = "分组名称", example = "A栋1-5层商户")
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    @Schema(description = "分组管理类型：BILLING=计费管理，METERING=计量管理", example = "METERING", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组管理类型不能为空")
    @Pattern(regexp = "BILLING|METERING", message = "分组管理类型只能是BILLING或METERING")
    private String manageType;

    @Schema(description = "统计维度：NORMAL/PROJECT/AREA/CATEGORY")
    @Pattern(regexp = "NORMAL|PROJECT|AREA|CATEGORY", message = "统计维度类型非法")
    private String dimensionType;

    @Schema(description = "父分组ID（不传=顶级分组，传值=子分组）", example = "1")
    private Long parentId;

    @Schema(description = "分组描述", example = "A栋低区商业用电分组")
    private String description;

    @Schema(description = "关联商户ID（仅ADMIN可指定，非ADMIN强制覆盖）")
    private Long merchantId;

    @Schema(description = "状态：0-停用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
