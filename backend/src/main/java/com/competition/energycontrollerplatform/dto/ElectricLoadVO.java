package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用电负荷响应 VO。
 * <p>
 * 按日/月维度统计最大负荷、平均负荷、最小负荷及负荷率。
 * 数据来源：t_report_daily_electricity 预聚合表。
 */
@Data
@Schema(description = "用电负荷条目")
public class ElectricLoadVO {

    @Schema(description = "统计时段（日:yyyy-MM-dd, 月:yyyy-MM）")
    private String period;

    @Schema(description = "最大日用电量（kWh），代表该时段内单日最高负荷")
    private BigDecimal maxLoad;

    @Schema(description = "平均日用电量（kWh），代表该时段内日均负荷")
    private BigDecimal averageLoad;

    @Schema(description = "最小日用电量（kWh），代表该时段内单日最低负荷")
    private BigDecimal minLoad;

    @Schema(description = "负荷率（%）= 平均负荷 / 最大负荷 × 100，反映负荷均衡程度")
    private BigDecimal loadRate;

    @Schema(description = "统计天数")
    private Integer dayCount;

    @Schema(description = "涉及表计数")
    private Long meterCount;

    @Schema(description = "时段总用电量（kWh）")
    private BigDecimal totalConsumed;
}
