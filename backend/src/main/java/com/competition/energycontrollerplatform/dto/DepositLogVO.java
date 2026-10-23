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
 * 充值明细响应 VO（同时用于 JSON 响应和 Excel 导出）。
 * <p>所有金额字段均为 {@link BigDecimal}，杜绝浮点精度丢失。
 */
@Data
@Schema(description = "充值明细条目")
public class DepositLogVO {

    @ExcelIgnore
    @Schema(description = "充值订单ID")
    private Long id;

    @ExcelProperty("订单号")
    @Schema(description = "订单号")
    private String orderNo;

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

    @ExcelProperty("充值金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "充值金额（元）")
    private BigDecimal amount;

    @ExcelProperty("支付金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "支付金额（元）")
    private BigDecimal payAmount;

    @ExcelProperty("到账金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "到账金额（元）")
    private BigDecimal realAmount;

    @ExcelProperty("到账后余额（元）")
    @NumberFormat("#.##")
    @Schema(description = "到账后余额（元）")
    private BigDecimal balance;

    @ExcelIgnore
    @Schema(description = "充值类型：1-线上,2-人工,3-转账")
    private Integer chargeType;

    @ExcelProperty("收款类型")
    @Schema(description = "收款类型名称")
    private String chargeTypeLabel;

    @ExcelIgnore
    @Schema(description = "交易类型：1-充值电费,2-充值水费")
    private Integer tradeType;

    @ExcelProperty("交易类型")
    @Schema(description = "交易类型名称")
    private String tradeTypeLabel;

    @ExcelProperty("操作员")
    @Schema(description = "操作员")
    private String operatorName;

    @ExcelProperty("用户名")
    @Schema(description = "用户名/缴费人")
    private String userName;

    @ExcelProperty("项目名称")
    @Schema(description = "项目名称")
    private String proName;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    @ExcelProperty("充值时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Schema(description = "充值时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
