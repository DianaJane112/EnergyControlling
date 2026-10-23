package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 扫码查询表计信息响应（小程序端 — 只读，不执行绑定）。
 * <p>
 * 与 {@link MeterDetailVO} 的区别：额外携带绑定状态字段，
 * 帮助前端决定展示"可绑定"/"已绑定"/"已被他人绑定"三种 UI。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "扫码查询表计信息响应")
public class MeterScanResultVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号（dbNo）")
    private String meterNo;

    @Schema(description = "表计类型: ELECTRIC=电表, WATER=水表")
    private String meterType;

    @Schema(description = "表计名称")
    private String meterName;

    @Schema(description = "安装地址")
    private String address;

    @Schema(description = "当前余额（元），仅已绑定到当前商户时有值")
    private BigDecimal balance;

    @Schema(description = "运行状态")
    private String status;

    @Schema(description = "在线状态")
    private String onlineStatus;

    @Schema(description = "预警线（元）")
    private BigDecimal warnline;

    @Schema(description = "透支限额（元）")
    private BigDecimal credit;

    // ────────────── 绑定状态 ──────────────

    @Schema(description = "该表计是否已被某商户绑定")
    private Boolean isBound;

    @Schema(description = "该表计是否已绑定到当前扫码用户")
    private Boolean boundToCurrentUser;

    @Schema(description = "已绑定的商户名称（已绑定但非当前用户时展示）")
    private String boundCustomerName;
}
