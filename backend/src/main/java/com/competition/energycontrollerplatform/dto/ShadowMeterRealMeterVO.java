package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虚拟电表关联的真实电表响应 VO。
 */
@Data
@Schema(description = "虚拟电表关联的真实电表")
public class ShadowMeterRealMeterVO {

    @Schema(description = "关联关系ID（t_shadow_meter.id，用于解除关联）")
    private Long relationId;

    @Schema(description = "真实电表ID（t_meter.id）")
    private Long meterId;

    @Schema(description = "表号（钢印号或档案编号）")
    private String meterNo;

    @Schema(description = "商户名")
    private String meterName;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE")
    private String markStatus;

    @Schema(description = "供电状态（分合闸）：0=合闸, 1=分闸")
    private Integer powerStatus;

    @Schema(description = "通讯状态（在线）：0=离线, 1=在线")
    private Integer online;

    @Schema(description = "CT变比")
    private BigDecimal ctRatio;

    @Schema(description = "PT变比")
    private BigDecimal ptRatio;

    @Schema(description = "今日有功电量（kWh）")
    private BigDecimal todayActiveEnergy;

    @Schema(description = "状态更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
