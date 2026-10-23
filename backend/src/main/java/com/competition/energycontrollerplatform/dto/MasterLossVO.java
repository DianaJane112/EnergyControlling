package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 总表线损/损耗分析响应 VO。
 * <p>
 * 线损 = 总表读数 - 所有分表读数之和。
 * 损耗率 = 线损 / 总表读数 × 100%。
 */
@Data
@Schema(description = "总表线损分析结果")
public class MasterLossVO {

    @Schema(description = "总表ID")
    private Long masterMeterId;

    @Schema(description = "总表钢印号/表号")
    private String masterMeterNo;

    @Schema(description = "总表名称")
    private String masterMeterName;

    @Schema(description = "总表最新读数（kWh）")
    private BigDecimal masterReading;

    @Schema(description = "分表数量")
    private int subMeterCount;

    @Schema(description = "分表读数总和（kWh）")
    private BigDecimal subMetersTotalReading;

    @Schema(description = "绝对损耗量（kWh）= 总表读数 - 分表总和")
    private BigDecimal absoluteLoss;

    @Schema(description = "损耗比率（%）= 绝对损耗 / 总表读数 × 100，保留2位小数")
    private BigDecimal lossRatePercent;

    @Schema(description = "分表明细")
    private List<SubMeterReading> subMeters;

    /**
     * 计算损耗量。
     */
    public void calculateLoss() {
        if (masterReading == null) {
            masterReading = BigDecimal.ZERO;
        }
        if (subMetersTotalReading == null) {
            subMetersTotalReading = BigDecimal.ZERO;
        }
        this.absoluteLoss = masterReading.subtract(subMetersTotalReading);
        if (masterReading.compareTo(BigDecimal.ZERO) > 0) {
            this.lossRatePercent = absoluteLoss
                    .multiply(BigDecimal.valueOf(100))
                    .divide(masterReading, 2, RoundingMode.HALF_UP);
        } else {
            this.lossRatePercent = BigDecimal.ZERO;
        }
    }

    @Data
    @Schema(description = "分表读数明细")
    public static class SubMeterReading {

        @Schema(description = "分表ID")
        private Long meterId;

        @Schema(description = "分表钢印号/表号")
        private String meterNo;

        @Schema(description = "分表名称")
        private String meterName;

        @Schema(description = "最新读数（kWh）")
        private BigDecimal reading;

        @Schema(description = "系数")
        private BigDecimal coefficient;
    }
}
