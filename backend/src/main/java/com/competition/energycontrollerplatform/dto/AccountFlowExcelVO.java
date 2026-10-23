package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水 Excel 导出模型。
 * <p>
 * {@code @ExcelProperty} 的 value 即表格列头名称，
 * EasyExcel 根据字段顺序自动排列表格列序。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class AccountFlowExcelVO {

    @ExcelProperty("流水号")
    @ColumnWidth(30)
    private String flowNo;

    @ExcelProperty("商户ID")
    @ColumnWidth(12)
    private Long customerId;

    @ExcelProperty("变动前余额")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal balanceBefore;

    @ExcelProperty("变动金额")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal changeAmount;

    @ExcelProperty("变动后余额")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal balanceAfter;

    @ExcelProperty("交易类型")
    @ColumnWidth(14)
    private String tradeTypeLabel;

    @ExcelProperty("业务单号")
    @ColumnWidth(28)
    private String bizNo;

    @ExcelProperty("操作人")
    @ColumnWidth(14)
    private String operatorName;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime operateTime;

    /**
     * 交易类型编码（原始值，不写入 Excel，仅内部计算用）
     */
    @ExcelIgnore
    private Integer tradeType;
}
