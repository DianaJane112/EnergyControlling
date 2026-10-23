package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量修改电价请求 DTO。
 * <p>
 * 支持三种模式，每种模式均可按meterType过滤（仅电表/仅水表）：
 * <ul>
 *   <li>指定表计ID列表批量修改</li>
 *   <li>全局修改（meterIds为空且isGlobal=true，仅ADMIN）</li>
 *   <li>按商户批量修改（customerId非空）</li>
 * </ul>
 */
@Data
@Schema(description = "批量修改电价请求")
public class BatchUpdatePriceRequest {

    @Schema(description = "表计ID列表（为空+isGlobal=true表示全局修改）", example = "[1, 2, 3]")
    private List<Long> meterIds;

    @Schema(description = "是否全局修改（仅ADMIN可执行）", example = "false")
    private Boolean isGlobal;

    @Schema(description = "按商户ID批量修改（可选）", example = "1")
    private Long customerId;

    @Schema(description = "表计大类筛选：0=仅水表, 1=仅电表, null=全部（全局模式下用于限定表计类型）", example = "1")
    private Integer meterType;

    @Schema(description = "电价类型：1-固定电价, 2-费率电价, 3-阶梯电价", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "电价类型不能为空")
    private Integer priceType;

    @Schema(description = "固定电价（元/kWh），priceType=1时必填", example = "0.85")
    private BigDecimal fixedPrice;

    @Schema(description = "电价内容JSON（分时/阶梯电价时使用）", example = "[{\"slotName\":\"峰\",\"startHour\":8,\"endHour\":22,\"price\":1.00}]")
    private String priceContent;

    @Schema(description = "附加电费（元/kWh）", example = "0.00")
    private BigDecimal plusPrice;

    @Schema(description = "费率策略ID（关联t_price_strategy.id，可选）")
    private Long rateConfigId;
}
