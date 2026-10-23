package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 余额退补统一执行请求 DTO。
 * <p>
 * 串联"调整电价 → 选择表计 → 预览 → 执行"全流程于一个请求中。
 * 支持两种退补类型：
 * <ul>
 *   <li>PRICE_ADJUST — 调整电价：修改表计电价后，根据电价变化计算余额应退或应补金额并自动执行</li>
 *   <li>BALANCE_ADJUST — 余额调整：直接对表计关联商户账户进行余额增减，金额变动可为正或负</li>
 * </ul>
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "余额退补统一执行请求")
public class BalanceAdjustmentExecuteRequest {

    @Schema(description = "目标表计ID（与dbNo二选一，优先用dbNo）", example = "1")
    private Long meterId;

    @Schema(description = "表号/钢印号（12位字符串，与meterId二选一，优先用此字段）", example = "202309150001")
    private String dbNo;

    @Schema(description = "调整后电价（元/kWh），仅 PRICE_ADJUST 类型时使用。" +
            "若不传则与当前电价相同，即仅做余额调整", example = "0.85")
    private BigDecimal adjustedPrice;

    @NotNull(message = "金额变动不能为空")
    @Schema(description = "金额变动（元），正数=增加余额=退费，负数=扣减余额=补收",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    private BigDecimal amountChange;

    @NotBlank(message = "调整原因不能为空")
    @Schema(description = "调整原因（用于审计追溯）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "电价从0.8调整为0.85，补偿差额")
    private String reason;

    @Schema(description = "备注（选填）", example = "2026年5月批次电价调整")
    private String remark;

    @Schema(description = "执行类型：PRICE_ADJUST-调整电价并执行余额退补，" +
            "BALANCE_ADJUST-直接调整余额", example = "PRICE_ADJUST")
    private String executeType;

    @Schema(description = "是否仅预览（true=仅预览不执行，false=执行）", example = "false")
    private Boolean previewOnly;

    @Schema(description = "扣除方式：immediate-立即扣除, delayed-延期扣除", example = "immediate")
    private String deductMethod;
}
