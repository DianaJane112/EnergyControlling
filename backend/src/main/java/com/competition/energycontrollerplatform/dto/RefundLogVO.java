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
 * 退费明细响应 VO — 同时用于 JSON 响应和 Excel 导出。
 * <p>所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "退费明细条目")
public class RefundLogVO {

    @Schema(description = "退费日志ID")
    private Long id;

    @ExcelProperty("流水号")
    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "退费编号")
    private String refundNo;

    @Schema(description = "商户ID")
    private Long customerId;

    @ExcelProperty("商户名称")
    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "表计ID")
    private Long meterId;

    @ExcelProperty("退费前余额（元）")
    @NumberFormat("#.##")
    @Schema(description = "退费前余额（元）")
    private BigDecimal balanceBefore;

    @ExcelProperty("退费金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "退费金额（元）")
    private BigDecimal refundAmount;

    @ExcelProperty("退费后余额（元）")
    @NumberFormat("#.##")
    @Schema(description = "退费后余额（元）")
    private BigDecimal balanceAfter;

    @ExcelProperty("退费原因")
    @Schema(description = "退费原因")
    private String refundReason;

    @ExcelProperty("操作员")
    @Schema(description = "操作员姓名")
    private String operatorName;

    @ExcelProperty("退费时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "退费时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime operateTime;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;
}
