package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水/对账记录表
 * 每一笔充值、退费、调账都必须写入一条结构化流水，用于审计对账。
 */
@Data
@TableName("t_account_flow")
public class AccountFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号，全局唯一，UUID 生成 */
    @TableField("flow_no")
    private String flowNo;

    /** 关联商户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 关联表计ID（可选） */
    @TableField("meter_id")
    private Long meterId;

    /** 变动前余额 */
    @TableField("balance_before")
    private BigDecimal balanceBefore;

    /** 变动金额（正=入账，负=出账） */
    @TableField("change_amount")
    private BigDecimal changeAmount;

    /** 变动后余额 */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    /**
     * 交易类型：
     * 1-充值
     * 2-消费扣费
     * 3-退费
     * 4-调账
     */
    @TableField("trade_type")
    private Integer tradeType;

    /** 关联业务单号（充值订单号/退费编号/调整编号） */
    @TableField("biz_no")
    private String bizNo;

    /** 操作人 */
    @TableField("operator_name")
    private String operatorName;

    /** 操作时间 */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /** 备注/原因 */
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
