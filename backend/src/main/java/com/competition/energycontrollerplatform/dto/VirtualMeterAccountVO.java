package com.competition.energycontrollerplatform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VirtualMeterAccountVO {
    private Long virtualMeterId;
    private String virtualMeterNo;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String phone;
    private BigDecimal balance;
    private Integer status;
}
