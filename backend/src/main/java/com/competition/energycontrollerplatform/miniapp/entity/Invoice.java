package com.competition.energycontrollerplatform.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据实体。
 * <p>
 * 记录每笔充值的票据信息，用户可以在小程序端查看已开票/未开票的票据。
 * 票据可以由管理员在后台手动标记为"已开票"状态。
 */
@Data
@TableName("t_invoice")
public class Invoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 票据编号（格式: INV + 时间戳 + UUID前8位） */
    @TableField("invoice_no")
    private String invoiceNo;

    /** 商户ID（FK→t_customer.id） */
    @TableField("customer_id")
    private Long customerId;

    /** 表计ID（FK→t_meter.id，可为空） */
    @TableField("meter_id")
    private Long meterId;

    /** 关联充值订单ID（FK→t_recharge_order.id，可为空） */
    @TableField("recharge_order_id")
    private Long rechargeOrderId;

    /** 票据金额 */
    private BigDecimal amount;

    /** 票据类型: ELECTRIC=电费, WATER=水费 */
    @TableField("invoice_type")
    private String invoiceType;

    /** 开票状态: NOT_ISSUED=未开票, ISSUED=已开票 */
    @TableField("invoice_status")
    private String invoiceStatus;

    /** 开票时间 */
    @TableField("issue_time")
    private LocalDateTime issueTime;

    /** 备注 */
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
