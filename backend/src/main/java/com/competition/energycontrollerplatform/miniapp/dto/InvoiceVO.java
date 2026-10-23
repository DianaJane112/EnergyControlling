package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "票据响应")
public class InvoiceVO {

    @Schema(description = "票据ID")
    private Long id;

    @Schema(description = "票据编号")
    private String invoiceNo;

    @Schema(description = "票据类型: ELECTRIC=电费, WATER=水费")
    private String invoiceType;

    @Schema(description = "票据金额（元）")
    private BigDecimal amount;

    @Schema(description = "开票状态: NOT_ISSUED=未开票, ISSUED=已开票")
    private String invoiceStatus;

    @Schema(description = "开票时间")
    private LocalDateTime issueTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
