package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_meter_type")
public class MeterType {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_type_code")
    private String meterTypeCode;

    @TableField("meter_type")
    private String meterType;

    @TableField("meter_conn_mode")
    private String meterConnMode;

    @TableField("wave_rate")
    private String waveRate;

    private String caliber;

    private String minuent;

    @TableField("fact_code")
    private String factCode;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
