package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基站/集中器 Excel 导出模型。
 * <p>
 * 使用 EasyExcel 注解定义列头和格式，参考 {@link AccountFlowExcelVO} 样式。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class CollectorExcelVO {

    @ExcelProperty("集中器编号")
    @ColumnWidth(18)
    private String collCode;

    @ExcelProperty("集中器名称")
    @ColumnWidth(24)
    private String collName;

    @ExcelProperty("所属厂商代码")
    @ColumnWidth(18)
    private String factCode;

    @ExcelProperty("安装位置")
    @ColumnWidth(30)
    private String location;

    @ExcelProperty("区域代码")
    @ColumnWidth(16)
    private String areaCode;

    @ExcelProperty("关联商户")
    @ColumnWidth(22)
    private String customerName;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String memo;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime updateTime;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    private String createBy;
}
