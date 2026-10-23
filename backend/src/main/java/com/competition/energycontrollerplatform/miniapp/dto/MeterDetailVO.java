package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表计详情响应（小程序端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "表计详情响应")
public class MeterDetailVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号")
    private String meterNo;

    @Schema(description = "表计类型: ELECTRIC=电表, WATER=水表")
    private String meterType;

    @Schema(description = "表计名称")
    private String meterName;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "已使用量（kWh 或 m³）")
    private BigDecimal totalUsage;

    @Schema(description = "当前余额（元）")
    private BigDecimal balance;

    @Schema(description = "上次读数")
    private BigDecimal lastReading;

    @Schema(description = "上次读数时间")
    private LocalDateTime lastReadingTime;

    @Schema(description = "运行状态")
    private String status;

    @Schema(description = "在线状态")
    private String onlineStatus;

    @Schema(description = "安装地址")
    private String address;

    @Schema(description = "预警线（元）")
    private BigDecimal warnline;

    @Schema(description = "透支限额（元）")
    private BigDecimal credit;
}
