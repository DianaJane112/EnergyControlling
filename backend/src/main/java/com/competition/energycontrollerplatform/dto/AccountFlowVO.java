package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分账明细（资金流水）响应 VO — 同时用于 JSON 响应和 Excel 导出。
 * <p>所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "分账明细条目（资金流水）")
public class AccountFlowVO {

    @Schema(description = "流水ID")
    private Long id;

    @ExcelProperty("流水号")
    @Schema(description = "流水号（全局唯一）")
    private String flowNo;

    @Schema(description = "商户ID")
    private Long customerId;

    @ExcelProperty("商户名称")
    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "表计ID")
    private Long meterId;

    @ExcelProperty("变动前余额（元）")
    @NumberFormat("#.##")
    @Schema(description = "变动前余额（元）")
    private BigDecimal balanceBefore;

    @ExcelProperty("变动金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "变动金额（元，正=入账，负=出账）")
    private BigDecimal changeAmount;

    @ExcelProperty("变动后余额（元）")
    @NumberFormat("#.##")
    @Schema(description = "变动后余额（元）")
    private BigDecimal balanceAfter;

    @Schema(description = "交易类型编码：1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零,7-公共分摊")
    private Integer tradeType;

    @ExcelProperty("交易类型")
    @Schema(description = "交易类型名称")
    private String tradeTypeLabel;

    @ExcelProperty("业务单号")
    @Schema(description = "关联业务单号")
    private String bizNo;

    @ExcelProperty("操作员")
    @Schema(description = "操作人")
    private String operatorName;

    @ExcelProperty("备注")
    @Schema(description = "备注/原因")
    private String remark;

    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime operateTime;
}
