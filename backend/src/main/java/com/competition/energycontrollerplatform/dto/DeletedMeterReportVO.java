package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已删除表计月报表响应 VO。
 * <p>
 * 查询已逻辑删除的电表在指定月份的历史用电数据汇总。
 * 数据来源：tb_meter_monthly_report（月汇总）JOIN t_meter（已删除）
 *  + t_report_daily_electricity（每日明细，用于生成日列）。
 */
@Data
@Schema(description = "已删除表计月报表条目")
public class DeletedMeterReportVO {

    // ── 保留旧字段（向后兼容，导出等可能引用） ──

    @Schema(description = "统计月份（yyyy-MM）")
    private String statMonth;

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "尖时段用量（kWh）")
    private BigDecimal peakUsage;

    @Schema(description = "峰时段用量（kWh）")
    private BigDecimal busyUsage;

    @Schema(description = "平时段用量（kWh）")
    private BigDecimal normalUsage;

    @Schema(description = "谷时段用量（kWh）")
    private BigDecimal idleUsage;

    @Schema(description = "删除时间")
    private LocalDateTime deletedTime;

    @Schema(description = "统计天数")
    private Integer dailyCount;

    @Schema(description = "数据来源")
    private String dataSource;

    // ── 前端报表核心展示字段 ──

    @Schema(description = "设备类型（中文，如：电表）")
    private String deviceType;

    @Schema(description = "表号（设备号/钢印号）")
    private String deviceNo;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "月度总用量（kWh）")
    private BigDecimal totalUsage;

    @Schema(description = "每日用量 Map，key=MM-dd（如 06-01, 06-02, …, 06-11），value=当日用电量(kWh)")
    private Map<String, BigDecimal> dailyUsage;

    /**
     * 便捷方法：设置某一天的用电量。
     */
    public void putDaily(String dayKey, BigDecimal value) {
        if (this.dailyUsage == null) {
            this.dailyUsage = new LinkedHashMap<>();
        }
        this.dailyUsage.put(dayKey, value != null ? value : BigDecimal.ZERO);
    }
}
