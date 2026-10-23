package com.competition.energycontrollerplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VirtualMeterViewerRequest {
    @NotNull(message = "请选择查看用户")
    private Long miniappUserId;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String remark;
}
