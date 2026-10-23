package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 公共分摊统计响应 VO（同时用于 JSON 响应和 Excel 导出）。
 * <p>所有金额字段均为 {@link BigDecimal}，杜绝浮点精度丢失。</p>
 */
@Data
@Schema(description = "公共分摊统计条目（按日/月/年聚合）")
public class PublicAllocationSummaryVO {

    @ExcelProperty("统计时段")
    @Schema(description = "统计时段（日:yyyy-MM-dd, 月:yyyy-MM, 年:yyyy）")
    private String period;

    @ExcelProperty("分摊总金额（元）")
    @NumberFormat("#,##0.00")
    @Schema(description = "该时段分摊总金额（元）")
    private BigDecimal totalAllocatedAmount;

    @ExcelProperty("分摊次数")
    @Schema(description = "该时段分摊执行次数")
    private Long allocationCount;

    @ExcelProperty("平均单次分摊金额（元）")
    @NumberFormat("#,##0.00")
    @Schema(description = "该时段平均每次分摊金额（元）")
    private BigDecimal avgAmount;
}
