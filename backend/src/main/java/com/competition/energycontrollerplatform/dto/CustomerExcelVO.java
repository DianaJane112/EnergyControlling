package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * 商户信息 Excel 导出模型。
 * <p>
 * {@code @ExcelProperty} 的 value 即表格列头名称，
 * EasyExcel 根据字段顺序自动排列表格列序。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class CustomerExcelVO {

    @ExcelProperty("商户ID")
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty("用户编号")
    @ColumnWidth(18)
    private String customerCode;

    @ExcelProperty("商户名称")
    @ColumnWidth(22)
    private String customerName;

    @ExcelProperty("小区名称")
    @ColumnWidth(22)
    private String areaName;

    @ExcelProperty("地址")
    @ColumnWidth(36)
    private String address;

    @ExcelProperty("电话")
    @ColumnWidth(16)
    private String phone;

    @ExcelProperty("用户类型")
    @ColumnWidth(14)
    private String customerType;

    @ExcelProperty("用水性质")
    @ColumnWidth(14)
    private String useNature;

    @ExcelProperty("建档时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private java.time.LocalDateTime createDate;

    @ExcelProperty("建档类型")
    @ColumnWidth(12)
    private String type;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusLabel;

    @ExcelProperty("操作人")
    @ColumnWidth(14)
    private String operator;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private java.time.LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private java.time.LocalDateTime updateTime;
}
