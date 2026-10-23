package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_rate_config")
public class RateConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    @TableField("meter_type_code")
    private String meterTypeCode;

    @TableField("meter_type")
    private String meterType;

    @TableField("meter_conn_mode")
    private String meterConnMode;

    @TableField("wave_rate")
    private String waveRate;

    @TableField("fact_code")
    private String factCode;

    @TableField("price_type")
    private Integer priceType;

    @TableField("price_content")
    private String priceContent;

    @TableField("plus_price")
    private BigDecimal plusPrice;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expire_date")
    private LocalDate expireDate;

    private Integer status;

    private String remark;

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
