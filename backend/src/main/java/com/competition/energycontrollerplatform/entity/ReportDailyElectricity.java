package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用电量日报表实体（预聚合表）。
 * <p>
 * 每天凌晨 1:00 由 {@code ElectricityReportScheduler} 定时写入，
 * 前端报表查询直接读此表，避免全表扫描 t_meter_reading。
 */
@Data
@TableName("t_report_daily_electricity")
public class ReportDailyElectricity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报表日期 */
    @TableField("report_date")
    private LocalDate reportDate;

    /** 表计ID */
    @TableField("meter_id")
    private Long meterId;

    /** 商户ID（冗余） */
    @TableField("customer_id")
    private Long customerId;

    /** 分组ID（冗余，0=未分组） */
    @TableField("group_id")
    private Long groupId;

    /** 当日用电量（kWh） */
    @TableField("electricity_consumed")
    private BigDecimal electricityConsumed;

    /** 正向有功日用电量（kWh） */
    @TableField("forward_consumed")
    private BigDecimal forwardConsumed;

    /** 反向有功日用电量（kWh） */
    @TableField("reverse_consumed")
    private BigDecimal reverseConsumed;

    /** 尖时段用电量（kWh） */
    @TableField("peak_consumed")
    private BigDecimal peakConsumed;

    /** 峰时段用电量（kWh） */
    @TableField("busy_consumed")
    private BigDecimal busyConsumed;

    /** 平时段用电量（kWh） */
    @TableField("normal_consumed")
    private BigDecimal normalConsumed;

    /** 谷时段用电量（kWh） */
    @TableField("idle_consumed")
    private BigDecimal idleConsumed;

    /** 尖时段扣费金额（元） */
    @TableField("peak_charge_amount")
    private BigDecimal peakChargeAmount;

    /** 峰时段扣费金额（元） */
    @TableField("busy_charge_amount")
    private BigDecimal busyChargeAmount;

    /** 平时段扣费金额（元） */
    @TableField("normal_charge_amount")
    private BigDecimal normalChargeAmount;

    /** 谷时段扣费金额（元） */
    @TableField("idle_charge_amount")
    private BigDecimal idleChargeAmount;

    /** 当日扣费金额（元） */
    @TableField("charge_amount")
    private BigDecimal chargeAmount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
