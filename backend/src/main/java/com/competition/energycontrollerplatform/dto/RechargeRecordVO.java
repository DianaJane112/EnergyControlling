package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录 VO（用于虚拟电表"更多操作"中查询子表的充值记录）。
 */
@Data
@Schema(description = "充值记录")
public class RechargeRecordVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "表号")
    private String dbNo;

    @Schema(description = "充值金额")
    private BigDecimal amount;

    @Schema(description = "支付金额")
    private BigDecimal payamount;

    @Schema(description = "到账金额")
    private BigDecimal realamount;

    @Schema(description = "到账后余额")
    private BigDecimal balance;

    @Schema(description = "充值类型：1-线上, 2-人工, 3-转账")
    private Integer charegetype;

    @Schema(description = "手续费金额")
    private BigDecimal passrate;

    @Schema(description = "商户名")
    private String dbName;

    @Schema(description = "项目名称")
    private String proName;

    @Schema(description = "操作员")
    private String czy;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
