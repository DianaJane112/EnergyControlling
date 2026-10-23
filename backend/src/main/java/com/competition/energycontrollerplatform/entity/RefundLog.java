package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人工退费日志实体。
 * <p>
 * 每次人工退费操作均写入一条结构化日志，用于审计追溯。
 * 与 {@code t_refund_detail} 的区别：
 * <ul>
 *   <li>{@code t_refund_detail} — 业务退费单（退款类型、原订单等业务字段）</li>
 *   <li>{@code t_refund_log} — 退费资金流水（退费前/后余额、操作员等审计字段）</li>
 * </ul>
 */
@Data
@TableName("t_refund_log")
public class RefundLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号（全局唯一，UUID生成） */
    @TableField("flow_no")
    private String flowNo;

    /** 关联表计ID */
    @TableField("meter_id")
    private Long meterId;

    /** 关联商户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 退费前余额（元） */
    @TableField("balance_before")
    private BigDecimal balanceBefore;

    /** 退费金额（元，正数） */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    /** 退费后余额（元） */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    /** 操作员ID（关联t_user.id） */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作员姓名（冗余） */
    @TableField("operator_name")
    private String operatorName;

    /** 退费原因（必填） */
    @TableField("refund_reason")
    private String refundReason;

    /** 操作时间 */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /** 备注 */
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
