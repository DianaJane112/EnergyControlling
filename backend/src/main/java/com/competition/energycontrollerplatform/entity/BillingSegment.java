package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.*;

@Data
@TableName("t_billing_segment")
public class BillingSegment {
    @TableId(type = IdType.AUTO) private Long id;
    private String settlementNo;
    private Long virtualMeterId;
    private Long sourceMeterId;
    private Long customerId;
    private LocalDate billingDate;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal rawConsumption;
    private BigDecimal billedConsumption;
    private BigDecimal ctRatio;
    private BigDecimal ptRatio;
    private Long strategyId;
    private String strategyCode;
    private Integer strategyVersion;
    private Integer priceType;
    private BigDecimal unitPrice;
    private BigDecimal plusPrice;
    private BigDecimal chargeAmount;
    private Long projectId;
    private String projectName;
    private Long areaGroupId;
    private String areaName;
    private Long categoryGroupId;
    private String categoryName;
    private String detailJson;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
