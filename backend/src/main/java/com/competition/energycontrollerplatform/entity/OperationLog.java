package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("operator_name")
    private String operatorName;

    @TableField("module_name")
    private String moduleName;

    @TableField("operation_type")
    private String operationType;

    @TableField("operation_desc")
    private String operationDesc;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_result")
    private String responseResult;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("operation_time")
    private LocalDateTime operationTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
