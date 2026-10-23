package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_power_analysis")
public class PowerAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    @TableField("db_no")
    private String dbNo;

    private Double balance;

    @TableField("avg_cost_money")
    private Double avgCostMoney;

    @TableField("to_month_electricity")
    private Double toMonthElectricity;

    @TableField("to_day_electricity")
    private Double toDayElectricity;

    @TableField("sync_time")
    private LocalDateTime syncTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
