package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退费明细响应 VO（同时用于 JSON 响应和 Excel 导出）。
 * <p>数据来源：t_refund_detail 联查 t_customer、t_meter。
 * 所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "退费明细条目")
public class RefundDetailVO {

    @ExcelIgnore
    @Schema(description = "退费明细ID")
    private Long id;

    @ExcelProperty("退费编号")
    @Schema(description = "退费编号")
    private String refundNo;

    @ExcelIgnore
    @Schema(description = "商户ID")
    private Long customerId;

    @ExcelProperty("商户名称")
    @Schema(description = "商户名称")
    private String customerName;

    @ExcelIgnore
    @Schema(description = "表计ID")
    private Long meterId;

    @ExcelProperty("表号")
    @Schema(description = "表号")
    private String dbNo;

    @ExcelProperty("退费金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "退费金额（元）")
    private BigDecimal refundAmount;

    @ExcelProperty("退费类型")
    @Schema(description = "退费类型：PARTIAL_REFUND-部分退费, FULL_REFUND-全额退费")
    private String refundType;

    @ExcelProperty("退费类型（中文）")
    @Schema(description = "退费类型中文描述")
    private String refundTypeLabel;

    @ExcelProperty("退费原因")
    @Schema(description = "退费原因")
    private String refundReason;

    @ExcelIgnore
    @Schema(description = "关联原订单ID")
    private Long originalOrderId;

    @ExcelProperty("操作员")
    @Schema(description = "操作员姓名")
    private String operatorName;

    @ExcelProperty("退费时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "退费时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime refundTime;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
