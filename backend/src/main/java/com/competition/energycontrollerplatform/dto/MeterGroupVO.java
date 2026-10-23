package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分组管理响应 VO。
 */
@Data
@Schema(description = "分组信息")
public class MeterGroupVO {

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "父分组ID（null=顶级分组）")
    private Long parentId;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "分组管理类型：BILLING=计费管理，METERING=计量管理")
    private String manageType;

    private String dimensionType;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "关联商户ID")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "子分组列表（树形结构时填充）")
    private List<MeterGroupVO> children;

    private String groupPath;

    private Boolean hasChildren;
}
