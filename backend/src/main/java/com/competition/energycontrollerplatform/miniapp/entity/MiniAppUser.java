package com.competition.energycontrollerplatform.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序用户实体。
 * <p>
 * 支持微信登录（openid）和手机号登录（phone）两种方式，
 * 通过 customer_id 关联到商户档案，实现数据隔离。
 */
@Data
@TableName("t_miniapp_user")
public class MiniAppUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信OpenID（微信登录时必填） */
    @TableField("openid")
    private String openid;

    /** 微信UnionID（用于跨应用用户识别） */
    @TableField("unionid")
    private String unionid;

    /** 手机号（手机号登录时必填） */
    private String phone;

    /** 绑定的商户ID（FK→t_customer.id） */
    @TableField("customer_id")
    private Long customerId;

    /** 用户昵称 */
    private String nickname;

    /** 头像URL */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 登录方式：0=微信登录, 1=手机号登录 */
    @TableField("login_type")
    private Integer loginType;

    /** 状态：0=禁用, 1=正常 */
    private Integer status;

    /** 最后登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("token_version")
    private Integer tokenVersion;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
