package com.competition.energycontrollerplatform.dto;

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
 * 虚拟电表/水表（总分表拓扑）Excel 导出模型。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class ShadowMeterExcelVO {

    @ExcelProperty("关系ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("项目名称")
    @ColumnWidth(22)
    private String projectName;

    // ── 总表信息 ──

    @ExcelProperty("总表编号")
    @ColumnWidth(16)
    private String parentMeterNo;

    @ExcelProperty("总表名称")
    @ColumnWidth(20)
    private String parentMeterName;

    @ExcelProperty("总表类型")
    @ColumnWidth(10)
    private String parentMeterTypeDesc;

    // ── 子表信息 ──

    @ExcelProperty("子表编号")
    @ColumnWidth(16)
    private String childMeterNo;

    @ExcelProperty("子表名称")
    @ColumnWidth(20)
    private String childMeterName;

    @ExcelProperty("子表类型")
    @ColumnWidth(10)
    private String childMeterTypeDesc;

    @ExcelProperty("告警额度")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal childWarnline;

    @ExcelProperty("透支额度")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal childCredit;

    @ExcelProperty("付费方式")
    @ColumnWidth(10)
    private String childFeetypeDesc;

    @ExcelProperty("CT变比")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(12)
    private BigDecimal childCtRatio;

    @ExcelProperty("PT变比")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(12)
    private BigDecimal childPtRatio;

    @ExcelProperty("标记状态")
    @ColumnWidth(12)
    private String childMarkStatus;

    @ExcelProperty("通讯状态")
    @ColumnWidth(14)
    private String childCommStatusDesc;

    @ExcelProperty("停用警告")
    @ColumnWidth(10)
    private String childDisableWarningDesc;

    @ExcelProperty("运行状态")
    @ColumnWidth(10)
    private String childStatusDesc;

    @ExcelProperty("在线状态")
    @ColumnWidth(10)
    private String childOnlineDesc;

    // ── 拓扑关系 ──

    @ExcelProperty("影子类型")
    @ColumnWidth(12)
    private String shadowTypeDesc;

    @ExcelProperty("系数")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(10)
    private BigDecimal coefficient;

    @ExcelProperty("关系描述")
    @ColumnWidth(30)
    private String relationDesc;

    @ExcelProperty("生效日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate effectiveDate;

    @ExcelProperty("失效日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate expireDate;

    @ExcelProperty("状态")
    @ColumnWidth(8)
    private String statusDesc;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    private String createBy;
}
