package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("customer_code")
    private String customerCode;

    @TableField("customer_name")
    private String customerName;

    @TableField("area_name")
    private String areaName;

    private String address;

    private String phone;

    @TableField("customer_type")
    private String customerType;

    @TableField("use_nature")
    private String useNature;

    @TableField("create_date")
    private LocalDateTime createDate;

    private String type;

    @TableField("old_steal_no")
    private String oldStealNo;

    @TableField("old_val")
    private String oldVal;

    private Integer status;

    private String operator;

    private String sign;

    private String remark;

    @TableField("balance")
    private java.math.BigDecimal balance;

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
