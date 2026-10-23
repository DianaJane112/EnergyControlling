package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 即时账单列表响应 VO（扩展 InstantBillVO，增加列表展示字段）。
 */
@Data
@Schema(description = "即时账单列表条目")
@HeadRowHeight(22)
@ContentRowHeight(20)
public class InstantBillListVO {

    @ExcelProperty("表计ID")
    @ColumnWidth(12)
    @Schema(description = "表计ID")
    private Long meterId;

    @ExcelProperty("表计名称")
    @ColumnWidth(18)
    @Schema(description = "表计名称")
    private String meterName;

    @ExcelProperty("表号")
    @ColumnWidth(18)
    @Schema(description = "表号")
    private String meterNo;

    @ExcelProperty("商户ID")
    @ColumnWidth(12)
    @Schema(description = "商户ID")
    private Long customerId;

    @ExcelProperty("商户名称")
    @ColumnWidth(18)
    @Schema(description = "商户名称")
    private String customerName;

    @ExcelProperty("当前读数")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    @Schema(description = "当前读数")
    private BigDecimal currentReading;

    @ExcelProperty("上次结算读数")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    @Schema(description = "上次结算读数")
    private BigDecimal lastSettledReading;

    @ExcelProperty("消耗用量")
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    @Schema(description = "消耗用量")
    private BigDecimal consumption;

    @ExcelProperty("倍率")
    @ColumnWidth(10)
    @Schema(description = "倍率")
    private Integer multiplier;

    @ExcelProperty("单价(元/kWh)")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(14)
    @Schema(description = "费率单价（元/kWh）")
    private BigDecimal unitPrice;

    @ExcelProperty("账单金额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    @Schema(description = "账单金额（元）")
    private BigDecimal billAmount;

    @ExcelProperty("当前余额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    @Schema(description = "当前余额（元）")
    private BigDecimal currentBalance;

    @ExcelProperty("表计类型")
    @ColumnWidth(12)
    @Schema(description = "表计类型：0-水表，1-电表")
    private Integer meterType;

    @ExcelProperty("运行状态")
    @ColumnWidth(12)
    @Schema(description = "运行状态：0-停用，1-正常运行，2-欠费停机")
    private Integer status;

    @ExcelProperty("计算时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    @Schema(description = "计算时间")
    private LocalDateTime calculateTime;
}
