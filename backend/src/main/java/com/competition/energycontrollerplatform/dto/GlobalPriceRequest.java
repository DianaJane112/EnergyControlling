package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 全局修改单价请求 DTO（修改全园区所有电表或水表的单价）。
 * <p>
 * 仅 ADMIN 可执行。电表/水表分别使用独立接口。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "全局修改单价请求")
public class GlobalPriceRequest {

    @Schema(description = "电价/水价类型：1-固定单价, 2-费率单价, 3-阶梯单价", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单价类型不能为空")
    private Integer priceType;

    @Schema(description = "单价内容JSON（分时/阶梯单价时使用）", example = "[{\"slotName\":\"峰\",\"startHour\":8,\"endHour\":22,\"price\":1.00}]")
    private String priceContent;

    @Schema(description = "附加费（元/kWh 或 元/吨）", example = "0.00")
    private BigDecimal plusPrice;

    @Schema(description = "费率策略ID（关联t_price_strategy.id，可选）")
    private Long rateConfigId;
}
