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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公共分摊 Excel 导出模型。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class PublicAllocationExcelVO {

    @ExcelProperty("分摊ID")
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty("分摊名称")
    @ColumnWidth(26)
    private String allocationName;

    @ExcelProperty("分摊方式")
    @ColumnWidth(16)
    private String allocationTypeLabel;

    @ExcelProperty("分摊总金额")
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalAmount;

    @ExcelProperty("项目ID")
    @ColumnWidth(12)
    private Long projectId;

    @ExcelProperty("总表ID")
    @ColumnWidth(12)
    private Long sourceMeterId;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusLabel;

    @ExcelProperty("生效日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate effectiveDate;

    @ExcelProperty("到期日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate expireDate;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    private String createBy;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime updateTime;

    /**
     * 分摊方式编码（原始值，不写入 Excel）
     */
    @ExcelIgnore
    private String allocationType;

    /**
     * 状态编码（原始值，不写入 Excel）
     */
    @ExcelIgnore
    private Integer status;
}
