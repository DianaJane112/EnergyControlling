package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果 VO。
 * <p>
 * 用于包裹分页查询返回的列表数据与总记录数，
 * 前端可直接从 {@code data.list} 取列表、{@code data.total} 取总数。
 * </p>
 *
 * @param <T> 列表元素类型
 * @author EnergyControllerPlatform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResultVO<T> {

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "总记录数")
    private long total;

    public static <T> PageResultVO<T> of(List<T> list, long total) {
        return new PageResultVO<>(list, total);
    }
}
