package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用电负荷曲线数据点 VO。
 * <p>
 * 每条记录代表一天的用电负荷数据，按日期升序排列，
 * 前端可直接用于渲染 ECharts 折线图/面积图。
 * 数据来源：t_report_daily_electricity 预聚合表。
 */
@Data
@Schema(description = "用电负荷曲线数据点")
public class ElectricLoadCurveVO {

    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "当日总用电量（kWh），可作为主曲线")
    private BigDecimal totalLoad;

    @Schema(description = "尖时段用电量（kWh）")
    private BigDecimal peakLoad;

    @Schema(description = "峰时段用电量（kWh）")
    private BigDecimal busyLoad;

    @Schema(description = "平时段用电量（kWh）")
    private BigDecimal normalLoad;

    @Schema(description = "谷时段用电量（kWh）")
    private BigDecimal idleLoad;

    @Schema(description = "当日有数据的表计数")
    private Long meterCount;
}
