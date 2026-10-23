package com.competition.energycontrollerplatform.dto;

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
 * 账单 Excel 导出模型。
 * <p>
 * 使用 EasyExcel @ExcelProperty 注解定义中文列头，
 * 替代 BillController.export() 中硬编码 head/data 的方式。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class BillExportVO {

    @ExcelProperty("账单编号")
    @ColumnWidth(22)
    private String billNo;

    @ExcelProperty("账单名称")
    @ColumnWidth(20)
    private String billName;

    @ExcelProperty("账单类别")
    @ColumnWidth(14)
    private String categoryName;

    @ExcelProperty("商户名")
    @ColumnWidth(16)
    private String merchantName;

    @ExcelProperty("表号")
    @ColumnWidth(18)
    private String meterNo;

    @ExcelProperty("虚拟表商户名")
    @ColumnWidth(16)
    private String virtualMerchantName;

    @ExcelProperty("虚拟表号")
    @ColumnWidth(18)
    private String virtualMeterNo;

    @ExcelProperty("收款金额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal receivedAmount;

    @ExcelProperty("待收金额(元)")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal pendingAmount;

    @ExcelProperty("支付状态")
    @ColumnWidth(12)
    private String paymentStatus;

    @ExcelProperty("欠费处理")
    @ColumnWidth(12)
    private String arrearsHandling;

    @ExcelProperty("线上支付")
    @ColumnWidth(12)
    private String supportOnlinePaymentLabel;

    @ExcelProperty("描述")
    @ColumnWidth(24)
    private String description;

    @ExcelProperty("账单生成时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime billCreateTime;

    @ExcelProperty("最后支付时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime lastPaymentTime;

    @ExcelProperty("实际支付时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime actualPaymentTime;

    @ExcelProperty("备注")
    @ColumnWidth(20)
    private String remark;
}
