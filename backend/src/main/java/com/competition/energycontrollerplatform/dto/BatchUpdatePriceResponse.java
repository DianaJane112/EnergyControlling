package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 批量修改电价响应 DTO。
 */
@Data
@AllArgsConstructor
@Schema(description = "批量修改电价响应")
public class BatchUpdatePriceResponse {

    @Schema(description = "请求总数")
    private int total;

    @Schema(description = "成功数量")
    private int success;

    @Schema(description = "失败数量")
    private int failed;
}
