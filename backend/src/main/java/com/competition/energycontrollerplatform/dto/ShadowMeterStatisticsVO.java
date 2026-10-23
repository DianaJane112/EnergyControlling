package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 虚拟电表（总分表拓扑）统计汇总 VO。
 * <p>
 * 按项目、类型、状态等维度汇总拓扑关系数据。
 */
@Data
@Schema(description = "虚拟电表拓扑关系统计汇总")
public class ShadowMeterStatisticsVO {

    @Schema(description = "拓扑关系总数")
    private long totalCount;

    @Schema(description = "启用数量")
    private long activeCount;

    @Schema(description = "停用数量")
    private long inactiveCount;

    @Schema(description = "自动生成数量")
    private long autoCount;

    @Schema(description = "手动创建数量")
    private long manualCount;

    @Schema(description = "按项目分组统计：key=projectId, value=数量")
    private Map<Long, Long> byProject;

    @Schema(description = "按影子类型分组统计：key=shadowType, value=数量")
    private Map<String, Long> byShadowType;
}
