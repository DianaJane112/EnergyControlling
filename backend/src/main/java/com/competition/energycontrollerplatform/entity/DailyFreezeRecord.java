package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_daily_freeze_record")
public class DailyFreezeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    @TableField("customer_id")
    private Long customerId;

    @TableField("record_date")
    private LocalDate recordDate;

    private BigDecimal kwh;

    @TableField("kwhPeak")
    private BigDecimal kwhPeak;

    @TableField("kwhBusy")
    private BigDecimal kwhBusy;

    @TableField("kwhNormal")
    private BigDecimal kwhNormal;

    @TableField("kwhIdle")
    private BigDecimal kwhIdle;

    private BigDecimal zhyg;

    private BigDecimal zxyg;

    private BigDecimal fxyg;

    @TableField("read_val")
    private BigDecimal readVal;

    @TableField("freeze_time")
    private LocalDateTime freezeTime;

    @TableField("data_source")
    private String dataSource;

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
