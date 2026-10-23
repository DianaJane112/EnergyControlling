package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_balance_adjustment")
public class BalanceAdjustment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("adjustment_no")
    private String adjustmentNo;

    @TableField("meter_id")
    private Long meterId;

    @TableField("customer_id")
    private Long customerId;

    @TableField("db_no")
    private String dbNo;

    @TableField("balance")
    private BigDecimal balance;

    @TableField("adjustment_type")
    private String adjustmentType;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("original_surcharge")
    private BigDecimal originalSurcharge;

    @TableField("adjust_price")
    private BigDecimal adjustPrice;

    @TableField("adjust_surcharge")
    private BigDecimal adjustSurcharge;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("deduct_method")
    private String deductMethod;

    @TableField("deduct_time")
    private LocalDateTime deductTime;

    @TableField("group_name")
    private String groupName;

    @TableField("bz")
    private String bz;

    @TableField("operator_name")
    private String operatorName;

    @TableField("adjustment_time")
    private LocalDateTime adjustmentTime;

    private String sign;

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
