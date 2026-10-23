package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 基站/集中器统计响应 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集中器统计信息")
public class CollectorStatisticsVO {

    @Schema(description = "集中器总数")
    private Long totalCollectors;

    @Schema(description = "按区域分组统计")
    private List<AreaStat> areaStats;

    /**
     * 区域统计项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "区域统计项")
    public static class AreaStat {

        @Schema(description = "区域代码")
        private String areaCode;

        @Schema(description = "区域名称（与代码相同，可由前端映射）")
        private String areaName;

        @Schema(description = "该区域集中器数量")
        private Long count;
    }
}
