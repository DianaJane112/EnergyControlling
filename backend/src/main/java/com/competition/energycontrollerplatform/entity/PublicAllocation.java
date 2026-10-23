package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_public_allocation")
public class PublicAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("source_meter_id")
    private Long sourceMeterId;

    @TableField("allocation_name")
    private String allocationName;

    @TableField("allocation_type")
    private String allocationType;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("allocation_detail")
    private String allocationDetail;

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
