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
 * 电表厂商对接配置。
 */
@Data
@TableName("t_electric_meter_vendor")
public class ElectricMeterVendor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电表厂商名称 */
    @TableField("vendor_name")
    private String vendorName;

    /** API 基础地址 */
    @TableField("api_url")
    private String apiUrl;

    /** 应用 ID */
    @TableField("app_id")
    private String appId;

    /** 应用密钥 */
    @TableField("app_secret")
    private String appSecret;

    /** Webhook 签名密钥 */
    @TableField("webhook_signature")
    private String webhookSignature;

    /** 状态：0-停用, 1-启用 */
    private Integer status;

    /** 最近一次启用时间，用于确定当前优先对接配置 */
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
