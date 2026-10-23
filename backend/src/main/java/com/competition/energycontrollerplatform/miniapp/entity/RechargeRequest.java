package com.competition.energycontrollerplatform.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小程序充值申请实体。
 * <p>
 * 用户在小程序端发起虚拟充值申请，等待管理员在管理后台审核。
 * 审核通过后由管理员执行实际充值操作，关联充值订单号。
 * <p>
 * 审核流程: 用户提交 → PENDING → 管理员审核 → APPROVED / REJECTED
 */
@Data
@TableName("t_recharge_request")
public class RechargeRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请编号（格式: MPR + 时间戳 + UUID前8位） */
    @TableField("request_no")
    private String requestNo;

    /** 小程序用户ID（FK→t_miniapp_user.id） */
    @TableField("user_id")
    private Long userId;

    /** 商户ID（FK→t_customer.id） */
    @TableField("customer_id")
    private Long customerId;

    /** 表计ID（FK→t_meter.id） */
    @TableField("meter_id")
    private Long meterId;

    /** 充值金额 */
    private BigDecimal amount;

    /** 支付方式（固定为 VIRTUAL） */
    @TableField("pay_method")
    private String payMethod;

    /** 审核状态: PENDING=待审核, APPROVED=已通过, REJECTED=未通过 */
    @TableField("audit_status")
    private String auditStatus;

    /** 审核备注/拒绝原因 */
    @TableField("audit_remark")
    private String auditRemark;

    /** 审核人ID（FK→t_user.id，管理员） */
    @TableField("audit_user_id")
    private Long auditUserId;

    /** 审核时间 */
    @TableField("audit_time")
    private LocalDateTime auditTime;

    /** 关联充值订单号（审核通过后，管理员执行充值时生成的 t_recharge_order.order_no） */
    @TableField("related_order_no")
    private String relatedOrderNo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
