package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单记录实体。
 * <p>用于即时账单模块的账单持久化，与前端表格字段一一对应。</p>
 */
@Data
@TableName("t_bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账单编号（全局唯一） */
    @TableField("bill_no")
    private String billNo;

    /** 账单名称 */
    @TableField("bill_name")
    private String billName;

    /** 关联账单分类ID */
    @TableField("category_id")
    private Long categoryId;

    /** 账单类别名称（冗余） */
    @TableField("category_name")
    private String categoryName;

    /** 关联表计ID */
    @TableField("meter_id")
    private Long meterId;

    /** 关联小程序充值申请ID (FK→t_recharge_request.id) */
    @TableField("recharge_request_id")
    private Long rechargeRequestId;

    /** 表号/钢印号 */
    @TableField("meter_no")
    private String meterNo;

    /** 关联虚拟表计ID */
    @TableField("virtual_meter_id")
    private Long virtualMeterId;

    /** 虚拟表号 */
    @TableField("virtual_meter_no")
    private String virtualMeterNo;

    /** 商户名 */
    @TableField("merchant_name")
    private String merchantName;

    /** 虚拟表商户名 */
    @TableField("virtual_merchant_name")
    private String virtualMerchantName;

    /** 收款金额 */
    @TableField("received_amount")
    private BigDecimal receivedAmount;

    /** 待收金额 */
    @TableField("pending_amount")
    private BigDecimal pendingAmount;

    /** 支付状态：已支付/未支付/待审核 */
    @TableField("payment_status")
    private String paymentStatus;

    /** 欠费处理方式 */
    @TableField("arrears_handling")
    private String arrearsHandling;

    /** 是否支持线上支付 */
    @TableField("support_online_payment")
    private Integer supportOnlinePayment;

    /** 账单描述 */
    private String description;

    /** 账单生成时间 */
    @TableField("bill_create_time")
    private LocalDateTime billCreateTime;

    /** 最后支付时间 */
    @TableField("last_payment_time")
    private LocalDateTime lastPaymentTime;

    /** 实际支付时间 */
    @TableField("actual_payment_time")
    private LocalDateTime actualPaymentTime;

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
