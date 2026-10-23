package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值历史记录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "充值历史记录响应")
public class RechargeHistoryVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请编号")
    private String requestNo;

    @Schema(description = "表号")
    private String meterNo;

    @Schema(description = "表计名称")
    private String meterName;

    @Schema(description = "表计类型: ELECTRIC/WATER")
    private String meterType;

    @Schema(description = "充值金额（元）")
    private BigDecimal amount;

    @Schema(description = "支付方式")
    private String payMethod;

    @Schema(description = "审核状态: PENDING=待审核, APPROVED=已通过, REJECTED=未通过")
    private String auditStatus;

    @Schema(description = "审核备注/拒绝原因")
    private String auditRemark;

    @Schema(description = "关联充值订单号")
    private String relatedOrderNo;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
}
