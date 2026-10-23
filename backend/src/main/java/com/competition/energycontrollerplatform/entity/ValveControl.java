package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_valve_control")
public class ValveControl {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("meter_id")
    private Long meterId;

    private String meteraddress;

    private String companycode;

    private String customerno;

    private String sendbody;

    @TableField("ctrl_code1")
    private Integer ctrlCode1;

    @TableField("ctrl_code2")
    private Integer ctrlCode2;

    private String batchid;

    @TableField("task_status")
    private String taskStatus;

    private String type;

    @TableField("steal_no")
    private String stealNo;

    private String vender;

    private String operator;

    private String sign;

    @TableField("operate_date")
    private LocalDateTime operateDate;

    private String returnvalue;

    private String msg;

    @TableField("state_valve")
    private Integer stateValve;

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
