package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量修改表计信息请求 DTO。
 * <p>
 * 支持批量修改表号、分组、商户名、标记状态、停用警告、CT/PT变比等信息。
 */
@Data
@Schema(description = "批量修改表计信息请求")
public class BatchUpdateMeterRequest {

    @Schema(description = "表计ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "表计ID列表不能为空")
    private List<Long> meterIds;

    @Schema(description = "商户名（辰鳗name）")
    private String name;

    @Schema(description = "关联分组ID")
    private Long groupId;

    @Schema(description = "表计类型：1租户/2总表/3公共/4其他/5分摊表/6公区总表")
    private Integer bjlx;

    @Schema(description = "CT变比")
    private BigDecimal ctRatio;

    @Schema(description = "PT变比")
    private BigDecimal ptRatio;

    @Schema(description = "标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE")
    private String markStatus;

    @Schema(description = "是否停用警告：0-启用, 1-停用")
    private Integer disableWarning;

    @Schema(description = "通讯状态描述")
    private String commStatusDesc;

    @Schema(description = "运行状态：0-停用, 1-正常运行, 2-欠费停机")
    private Integer status;

    @Schema(description = "表计安装地址")
    private String address;
}
