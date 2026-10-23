package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 实时曲线数据点（专供 ECharts 折线图）
 */
@Data
@Schema(description = "实时曲线数据点")
public class CurvePointVO {

    @Schema(description = "采集时间戳（毫秒）")
    private Long timestamp;

    @Schema(description = "A相电压（V）")
    private BigDecimal voltage;
    private BigDecimal voltageB;
    private BigDecimal voltageC;

    @Schema(description = "A相电流（A）")
    private BigDecimal current;
    private BigDecimal currentB;
    private BigDecimal currentC;

    @Schema(description = "总有功功率（W）")
    private BigDecimal power;

    @Schema(description = "组合有功总电量（kWh）")
    private BigDecimal kwh;
}
