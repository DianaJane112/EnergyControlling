package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 银行账户设置实体。
 * <p>用于即时账单模块的银行账户管理，支持增删改查。</p>
 */
@Data
@TableName("t_bank_account")
public class BankAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 银行名称 */
    @TableField("bank_name")
    private String bankName;

    /** 账户名称 */
    @TableField("account_name")
    private String accountName;

    /** 银行账号 */
    @TableField("account_number")
    private String accountNumber;

    /** 开户支行 */
    @TableField("bank_branch")
    private String bankBranch;

    /** 开户行联行号 */
    @TableField("bank_code")
    private String bankCode;

    /** 账户类型：COMPANY-对公账户，PERSONAL-个人账户 */
    @TableField("account_type")
    private String accountType;

    /** 是否默认账户：0-否，1-是 */
    @TableField("is_default")
    private Integer isDefault;

    /** 联系人 */
    @TableField("contact_person")
    private String contactPerson;

    /** 联系电话 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 备注 */
    private String remark;

    /** 状态：0-停用，1-启用 */
    private Integer status;

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
