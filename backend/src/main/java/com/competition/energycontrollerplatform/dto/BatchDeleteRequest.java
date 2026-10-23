package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求 DTO。
 * <p>使用包装对象而非 {@code List<Long>} 作为 {@code @RequestBody}，
 * 避免 Jackson 泛型擦除导致的 JSON 数组反序列化失败。</p>
 */
@Data
@Schema(description = "批量删除请求")
public class BatchDeleteRequest {

    @Schema(description = "要删除的 ID 列表", example = "[1, 2, 3]")
    private List<Long> ids;
}
