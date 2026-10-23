package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Water meter vendor integration configuration.
 */
@Data
@TableName("t_water_meter_vendor")
public class WaterMeterVendor {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("vendor_name")
    private String vendorName;

    /**
     * Vendor identifier, for example JICHENG, JIEXIAN, HAOPU, CUSTOM.
     */
    @TableField("vendor_code")
    private String vendorCode;

    /**
     * Protocol adapter key, for example JICHENG_HTTP, JICHENG_TCP, JIEXIAN_HTTP.
     */
    @TableField("protocol_type")
    private String protocolType;

    @TableField("api_url")
    private String apiUrl;

    /**
     * Authentication type, for example SIGN, APP_SECRET, COMPANY_CODE, TOKEN, NONE.
     */
    @TableField("auth_type")
    private String authType;

    @TableField("app_id")
    private String appId;

    @TableField("app_secret")
    private String appSecret;

    @TableField("webhook_signature")
    private String webhookSignature;

    /**
     * Vendor-specific JSON configuration, such as sign/companyCode/tcpPort.
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 0 disabled, 1 enabled.
     */
    private Integer status;

    @TableField("last_enabled_time")
    private LocalDateTime lastEnabledTime;

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
