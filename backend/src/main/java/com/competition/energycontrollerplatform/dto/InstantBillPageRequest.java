package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 即时账单分页查询请求。
 */
@Data
@Schema(description = "即时账单分页查询请求")
public class InstantBillPageRequest {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "20", defaultValue = "20")
    private Integer pageSize;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制覆盖）")
    private Long customerId;

    @Schema(description = "表计号/钢印号（模糊搜索）")
    private String meterNo;

    @Schema(description = "商户名称（模糊搜索）")
    private String customerName;

    @Schema(description = "账单分类ID")
    private Long categoryId;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "账单状态：UNPAID-未缴，PAID-已缴，OVERDUE-逾期")
    private String billStatus;

    @Schema(description = "最小账单金额")
    private java.math.BigDecimal minAmount;

    @Schema(description = "最大账单金额")
    private java.math.BigDecimal maxAmount;
}
