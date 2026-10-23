package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_virtual_meter_viewer")
public class VirtualMeterViewer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long virtualMeterId;
    private Long miniappUserId;
    private String permissionType;
    private Integer status;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    @TableLogic
    private Integer isDeleted;
}
