package com.competition.energycontrollerplatform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VirtualMeterViewerVO {
    private Long id;
    private Long virtualMeterId;
    private Long miniappUserId;
    private String nickname;
    private String phone;
    private String permissionType;
    private Integer status;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String remark;
    private LocalDateTime createTime;
}
