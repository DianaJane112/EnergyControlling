package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单分类实体。
 * <p>用于即时账单模块的账单分类管理，支持新增、删除、分页查询。</p>
 */
@Data
@TableName("t_bill_category")
public class BillCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    @TableField("category_name")
    private String categoryName;

    /** 分类编码（唯一） */
    @TableField("category_code")
    private String categoryCode;

    /** 分类描述 */
    private String description;

    /** 排序号 */
    @TableField("sort_order")
    private Integer sortOrder;

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
