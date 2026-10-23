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
 * 余额调整响应 VO（同时用于 JSON 响应和 Excel 导出）。
 * <p>所有金额字段均为 {@link BigDecimal}，杜绝浮点精度丢失。
 */
@Data
@Schema(description = "余额调整条目")
public class BalanceAdjustmentVO {

    @ExcelIgnore
    @Schema(description = "调整记录ID")
    private Long id;

    @ExcelProperty("调整编号")
    @Schema(description = "调整编号")
    private String adjustmentNo;

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

    @ExcelProperty("调整金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "调整金额（元），正数=增加，负数=扣减")
    private BigDecimal balance;

    @ExcelProperty("原电价")
    @NumberFormat("#.####")
    @Schema(description = "原电价（元/kWh）")
    private BigDecimal originalPrice;

    @ExcelProperty("原附加电价")
    @NumberFormat("#.####")
    @Schema(description = "原附加电价（元/kWh）")
    private BigDecimal originalSurcharge;

    @ExcelProperty("退补电价")
    @NumberFormat("#.####")
    @Schema(description = "退补电价（元/kWh）")
    private BigDecimal adjustPrice;

    @ExcelProperty("退补附加电价")
    @NumberFormat("#.####")
    @Schema(description = "退补附加电价（元/kWh）")
    private BigDecimal adjustSurcharge;

    @ExcelProperty("支付状态")
    @Schema(description = "支付状态：已完成/处理中")
    private String paymentStatus;

    @ExcelProperty("扣除方式")
    @Schema(description = "扣除方式：余额扣除/延期扣除")
    private String deductMethod;

    @ExcelProperty("扣除时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扣除时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime deductTime;

    @ExcelProperty("分组")
    @Schema(description = "分组名称")
    private String groupName;

    @ExcelProperty("调整类型")
    @Schema(description = "调整类型：INCREASE-余额增加, DECREASE-余额扣减")
    private String adjustmentType;

    @ExcelProperty("调整类型（中文）")
    @Schema(description = "调整类型中文描述")
    private String adjustmentTypeLabel;

    @ExcelProperty("调账原因")
    @Schema(description = "调账原因")
    private String bz;

    @ExcelProperty("操作员")
    @Schema(description = "操作员姓名")
    private String operatorName;

    @ExcelProperty("调整时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "调整时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime adjustmentTime;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
