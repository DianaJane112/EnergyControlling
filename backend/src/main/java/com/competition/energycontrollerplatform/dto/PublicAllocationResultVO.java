package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公共分摊执行结果 VO。
 */
@Data
@Schema(description = "公共分摊执行结果")
public class PublicAllocationResultVO {

    @Schema(description = "分摊记录ID")
    private Long allocationId;

    @Schema(description = "分摊名称")
    private String allocationName;

    @Schema(description = "分摊总金额（元）")
    private BigDecimal totalAmount;

    @Schema(description = "分摊策略")
    private String allocationType;

    @Schema(description = "成功扣减户数")
    private int successCount;

    @Schema(description = "失败户数")
    private int failCount;

    @Schema(description = "各表分摊明细")
    private List<AllocationItem> items;

    @Data
    @Schema(description = "单表分摊明细条目")
    public static class AllocationItem {

        @Schema(description = "电表ID")
        private Long meterId;

        @Schema(description = "电表钢印号/表号")
        private String meterNo;

        @Schema(description = "商户ID")
        private Long customerId;

        @Schema(description = "商户名称")
        private String customerName;

        @Schema(description = "分摊金额（元）")
        private BigDecimal allocatedAmount;

        @Schema(description = "分摊比例")
        private BigDecimal ratio;

        @Schema(description = "扣减前余额（元）")
        private BigDecimal balanceBefore;

        @Schema(description = "扣减后余额（元）")
        private BigDecimal balanceAfter;

        @Schema(description = "是否成功")
        private Boolean success;

        @Schema(description = "失败原因")
        private String failReason;
    }
}
