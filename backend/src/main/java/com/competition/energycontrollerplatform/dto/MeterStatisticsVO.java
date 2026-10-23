package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 表计统计汇总 VO。
 * <p>
 * 用于前端仪表盘/统计卡片展示表计总数及各类别分布。
 */
@Data
@Schema(description = "表计统计汇总")
public class MeterStatisticsVO {

    @Schema(description = "表计总数")
    private long totalCount;

    @Schema(description = "电表数量")
    private long electricCount;

    @Schema(description = "水表数量")
    private long waterCount;

    @Schema(description = "在线数量")
    private long onlineCount;

    @Schema(description = "离线数量")
    private long offlineCount;

    @Schema(description = "按运行状态分组统计：key=status(0-停用,1-正常运行,2-欠费停机), value=数量")
    private Map<Integer, Long> byStatus;

    @Schema(description = "按表计业务类型分组统计：key=bjlx(1-租户,2-总表,3-公共,4-其他,5-分摊表,6-公区总表), value=数量")
    private Map<Integer, Long> byBjlx;

    @Schema(description = "按标记状态分组统计：key=markStatus(NORMAL/ATTENTION/ABNORMAL/MAINTENANCE), value=数量")
    private Map<String, Long> byMarkStatus;

    @Schema(description = "按通讯状态分组统计：key=commStatusDesc, value=数量")
    private Map<String, Long> byCommStatus;
}
