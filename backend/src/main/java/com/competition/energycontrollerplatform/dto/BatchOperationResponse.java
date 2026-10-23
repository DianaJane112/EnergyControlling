package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量操作通用响应 DTO。
 */
@Data
@NoArgsConstructor
@Schema(description = "批量操作通用响应")
public class BatchOperationResponse {

    @Schema(description = "请求总数")
    private int total;

    @Schema(description = "成功数量")
    private int success;

    @Schema(description = "失败数量")
    private int failed;

    @Schema(description = "操作描述")
    private String message;

    @Schema(description = "错误详情列表")
    private List<String> errors;

    /**
     * 四参数构造器（保持与旧代码兼容）。
     */
    public BatchOperationResponse(int total, int success, int failed, String message) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.message = message;
    }
}
