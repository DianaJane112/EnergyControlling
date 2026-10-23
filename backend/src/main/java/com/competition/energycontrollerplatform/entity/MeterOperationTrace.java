package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_meter_operation_trace")
public class MeterOperationTrace {
    @TableId(type = IdType.AUTO) private Long id;
    private String objectType; private Long objectId; private String objectNo; private Long projectId;
    private Long customerId; private String customerCodeSnapshot; private String customerNameSnapshot;
    private Long meterId; private Long parentMeterId; private Long childMeterId; private String eventType;
    private String operationTerminal; private Long operatorId; private String operatorName;
    private BigDecimal balanceBefore; private BigDecimal changeAmount; private BigDecimal creditedAmount; private BigDecimal balanceAfter;
    private BigDecimal previousReading; private BigDecimal rawMeasurement; private BigDecimal billedMeasurement; private BigDecimal consumption;
    private BigDecimal ctRatio; private BigDecimal ptRatio; private BigDecimal unitPrice; private BigDecimal plusPrice;
    private Long rateConfigId; private Integer priceType;
    private String billingType; private String paymentMethod; private String relationBefore; private String relationAfter;
    private String remark; private String requestId; private LocalDateTime operationTime; private String resultStatus; private String failureReason;
    private String detailJson; private LocalDateTime createTime; @TableLogic private Integer isDeleted;
}
