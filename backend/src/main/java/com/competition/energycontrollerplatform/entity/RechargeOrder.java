package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_recharge_order")
public class RechargeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    @TableField("customer_id")
    private Long customerId;

    @TableField("order_no")
    private String orderNo;

    @TableField("db_no")
    private String dbNo;

    private BigDecimal amount;

    private BigDecimal payamount;

    private BigDecimal realamount;

    private BigDecimal balance;

    private Integer charegetype;

    private BigDecimal passrate;

    private BigDecimal srvfee;

    private Integer srvtpe;

    private Integer srvpaid;

    private String czy;

    @TableField("db_name")
    private String dbName;

    @TableField("pro_name")
    private String proName;

    private String sign;

    @TableField("operator_name")
    private String operatorName;

    @TableField("user_name")
    private String userName;

    @TableField("trade_type")
    private Integer tradeType;

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
