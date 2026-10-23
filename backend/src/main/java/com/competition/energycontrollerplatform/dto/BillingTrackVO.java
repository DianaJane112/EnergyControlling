package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计费轨迹 VO（用于虚拟电表"更多操作"中查询子表的资金流水/消费记录）。
 * <p>
 * 展示指定表计的每一笔消费扣费明细，包括扣费前后的余额变化。
 */
@Data
@Schema(description = "计费轨迹（资金流水明细）")
public class BillingTrackVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "变动前余额")
    private BigDecimal balanceBefore;

    @Schema(description = "变动金额（正=入账，负=出账）")
    private BigDecimal changeAmount;

    @Schema(description = "变动后余额")
    private BigDecimal balanceAfter;

    @Schema(description = "交易类型：1-充值, 2-消费扣费, 3-退费, 4-调账, 5-人工缴费, 6-余额清零, 7-公共分摊")
    private Integer tradeType;

    @Schema(description = "交易类型名称")
    private String tradeTypeName;

    @Schema(description = "关联业务单号")
    private String bizNo;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime operateTime;

    @Schema(description = "备注")
    private String remark;
}
