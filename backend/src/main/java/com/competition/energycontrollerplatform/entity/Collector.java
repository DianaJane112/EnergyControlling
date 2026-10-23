package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_collector")
public class Collector {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商户ID（平台侧权限隔离字段） */
    @TableField("customer_id")
    private Long customerId;

    @TableField("coll_code")
    private String collCode;

    @TableField("coll_name")
    private String collName;

    @TableField("fact_code")
    private String factCode;

    private String location;

    @TableField("area_code")
    private String areaCode;

    private String memo;

    private String sign;

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
