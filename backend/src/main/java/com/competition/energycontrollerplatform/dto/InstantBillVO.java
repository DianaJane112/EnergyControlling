package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 即时账单响应 VO。
 * <p>
 * 实时计算：(当前读数 - 上次结算读数) × 费率单价 × 倍率。
 * 所有金额字段使用 {@link BigDecimal}。
 */
@Data
@Schema(description = "即时账单")
public class InstantBillVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号")
    private String meterNo;

    @Schema(description = "商户ID")
    private Long customerId;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "当前读数（最新一条 t_meter_reading）")
    private BigDecimal currentReading;

    @Schema(description = "上次结算读数（来自 t_meter.last_settled_reading）")
    private BigDecimal lastSettledReading;

    @Schema(description = "消耗用量 = 当前读数 - 上次结算读数")
    private BigDecimal consumption;

    @Schema(description = "倍率")
    private Integer multiplier;

    @Schema(description = "费率单价（元/kWh）")
    private BigDecimal unitPrice;

    @Schema(description = "实时账单金额（元）= 消耗用量 × 倍率 × 单价")
    private BigDecimal billAmount;

    @Schema(description = "当前账户余额（元）")
    private BigDecimal currentBalance;

    @Schema(description = "计算时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime calculateTime;
}
