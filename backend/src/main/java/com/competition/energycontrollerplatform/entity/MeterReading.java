package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_meter_reading")
public class MeterReading {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    // 电表采集简述字段
    private Boolean report;

    @TableField("collectorCode")
    private Long collectorCode;

    @TableField("meterCode")
    private Long meterCode;

    private LocalDateTime timestamp;

    @TableField("voltA")
    private BigDecimal voltA;

    @TableField("voltB")
    private BigDecimal voltB;

    @TableField("voltC")
    private BigDecimal voltC;

    private BigDecimal freq;

    @TableField("ampA")
    private BigDecimal ampA;

    @TableField("ampB")
    private BigDecimal ampB;

    @TableField("ampC")
    private BigDecimal ampC;

    private BigDecimal fact;

    @TableField("factA")
    private BigDecimal factA;

    @TableField("factB")
    private BigDecimal factB;

    @TableField("factC")
    private BigDecimal factC;

    private BigDecimal act;

    @TableField("actA")
    private BigDecimal actA;

    @TableField("actB")
    private BigDecimal actB;

    @TableField("actC")
    private BigDecimal actC;

    private BigDecimal react;

    @TableField("reactA")
    private BigDecimal reactA;

    @TableField("reactB")
    private BigDecimal reactB;

    @TableField("reactC")
    private BigDecimal reactC;

    private BigDecimal kwh;

    @TableField("kwhPeak")
    private BigDecimal kwhPeak;

    @TableField("kwhBusy")
    private BigDecimal kwhBusy;

    @TableField("kwhNormal")
    private BigDecimal kwhNormal;

    @TableField("kwhIdle")
    private BigDecimal kwhIdle;

    // 辰鳗getDbRealTime接口字段
    @TableField("collector_device_no")
    private String collectorDeviceNo;

    private Integer phase;

    private Double poweryg;

    private Double poweryga;

    private Double powerygb;

    private Double powerygc;

    private Double powerwg;

    private Double powerwga;

    private Double powerwgb;

    private Double powerwgc;

    private Double powerrate;

    private Double powerratea;

    private Double powerrateb;

    private Double powerratec;

    private Double frequency;

    private Double zhyg;

    private Double zxyg;

    private Double fxyg;

    private Double zxwg;

    private Double fxwg;

    private Double dl;

    private Double dlb;

    private Double dlc;

    private Double dy;

    private Double dyb;

    private Double dyc;

    @TableField("time_interval")
    private Integer timeInterval;

    @TableField("client_ip")
    private String clientIp;

    private LocalDateTime gxsj;

    // 积成GetMeterReading / 捷先getDataValue接口字段
    @TableField("customer_code")
    private String customerCode;

    @TableField("steal_no")
    private String stealNo;

    private String vender;

    @TableField("read_date")
    private LocalDateTime readDate;

    @TableField("read_val")
    private BigDecimal readVal;

    @TableField("read_jval")
    private BigDecimal readJval;

    private String meteraddress;

    @TableField("data_dt")
    private String dataDt;

    @TableField("data_value0")
    private Integer dataValue0;

    // 审计字段
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
