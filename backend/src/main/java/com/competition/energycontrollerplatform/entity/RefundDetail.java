package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_refund_detail")
public class RefundDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("meter_id")
    private Long meterId;

    @TableField("customer_id")
    private Long customerId;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("refund_type")
    private String refundType;

    @TableField("refund_reason")
    private String refundReason;

    @TableField("original_order_id")
    private Long originalOrderId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("refund_time")
    private LocalDateTime refundTime;

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
