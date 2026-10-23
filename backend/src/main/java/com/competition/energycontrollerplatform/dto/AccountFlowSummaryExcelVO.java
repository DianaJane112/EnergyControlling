package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分账统计汇总 Excel 导出模型。
 * <p>
 * 对应分账统计页面（按日/月/年聚合）的导出数据。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class AccountFlowSummaryExcelVO {

    @ExcelProperty("统计时段")
    @ColumnWidth(18)
    private String period;

    @ExcelProperty("充值总额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalRecharge;

    @ExcelProperty("扣费总额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalDebit;

    @ExcelProperty("退费总额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalRefund;

    @ExcelProperty("调账总额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalAdjust;

    @ExcelProperty("分摊总额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalAllocation;

    @ExcelProperty("净流水(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal netFlow;

    @ExcelProperty("笔数")
    @ColumnWidth(12)
    private Long flowCount;
}
