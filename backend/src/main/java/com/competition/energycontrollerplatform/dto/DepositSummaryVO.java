package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值统计响应 VO（同时用于 JSON 响应和 Excel 导出）。
 * <p>所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "充值统计条目（按日/月/年聚合）")
public class DepositSummaryVO {

    @ExcelProperty("统计时段")
    @Schema(description = "统计时段（日:yyyy-MM-dd, 月:yyyy-MM, 年:yyyy）")
    private String period;

    @ExcelProperty("充值总金额（元）")
    @NumberFormat("#.##")
    @Schema(description = "该时段充值总金额（元）")
    private BigDecimal totalAmount;

    @ExcelProperty("充值笔数")
    @Schema(description = "该时段充值笔数")
    private Long chargeCount;

    @ExcelProperty("涉及商户数")
    @Schema(description = "该时段涉及的商户数")
    private Long customerCount;
}
