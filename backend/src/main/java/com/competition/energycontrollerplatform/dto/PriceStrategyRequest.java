package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费率时段策略请求 DTO（新增/编辑共用）。
 */
@Data
@Schema(description = "费率时段策略请求")
public class PriceStrategyRequest {

    @Schema(description = "策略名称", example = "夏季工商业分时电价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    @Schema(description = "策略编码（唯一）", example = "SUMMER_COMMERCIAL_TOU", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "策略编码不能为空")
    private String strategyCode;

    @Schema(description = "费率版本组编码；同一组内按生效日期选择版本")
    private String versionGroupCode;

    @Schema(description = "关联项目ID（NULL=全局策略）")
    private Long projectId;

    @Schema(description = "表计类型：0-水表，1-电表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表计类型不能为空")
    private Integer meterType;

    @Schema(description = "策略类型：1-固定单价, 2-分时电价(尖峰平谷), 3-阶梯电价", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "策略类型不能为空")
    private Integer priceType;

    @Schema(description = "时段电价JSON：分时电价[{slotName,startHour,endHour,price}]，阶梯电价[{amount,price}]", example = "[{\"slotName\":\"尖\",\"startHour\":19,\"endHour\":21,\"price\":1.20}]")
    private String timeSlots;

    @Schema(description = "固定单价（priceType=1时必填）", example = "0.85")
    private BigDecimal fixedPrice;

    @Schema(description = "附加费（元/kWh）", example = "0.00")
    private BigDecimal plusPrice;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期（NULL=永久有效）")
    private LocalDate expireDate;

    @Schema(description = "状态：0-停用, 1-启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
