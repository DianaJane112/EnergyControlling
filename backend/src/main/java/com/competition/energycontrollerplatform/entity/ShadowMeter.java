package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_shadow_meter")
public class ShadowMeter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("parent_meter_id")
    private Long parentMeterId;

    @TableField("child_meter_id")
    private Long childMeterId;

    @TableField("shadow_type")
    private String shadowType;

    private BigDecimal coefficient;

    @TableField("relation_desc")
    private String relationDesc;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expire_date")
    private LocalDate expireDate;

    private Integer status;

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
