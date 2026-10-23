package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分组管理实体。
 * <p>
 * 用于电表分组管理，支撑报表系统中"按分组统计用电量"维度。
 * 通过 merchant_id 实现商户级数据权限隔离。
 * 通过 parent_id 支持多级层级结构（顶级分组 parent_id = NULL）。
 *
 * @author EnergyControllerPlatform
 */
@Data
@TableName("t_meter_group")
public class MeterGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分组ID，NULL 表示顶级分组 */
    @TableField("parent_id")
    private Long parentId;

    @TableField("group_name")
    private String groupName;

    /** 分组管理类型：BILLING=计费管理，METERING=计量管理。 */
    @TableField("manage_type")
    private String manageType;

    /** NORMAL / PROJECT / AREA / CATEGORY. */
    @TableField("dimension_type")
    private String dimensionType;

    @TableField("description")
    private String description;

    /** 关联商户ID — 数据权限隔离核心字段 */
    @TableField("merchant_id")
    private Long merchantId;

    private Integer status;

    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
