package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 已删除水表月报表响应 VO。
 * <p>
 * <b>业务背景：</b>园区商户退租或设备损坏后，部分水表会被逻辑删除（is_deleted=1）
 * 或移入历史归档表。但财务对总账时仍需统计这些已删除水表在历史月份中的用水量。
 * </p>
 * <p>
 * <b>数据来源：</b>水表读数流水表 INNER JOIN 已删除水表（is_deleted=1），
 * 按月+水表维度实时聚合。自定义 XML SQL 直接写 WHERE wm.is_deleted = 1，
 * 不受 MyBatis-Plus {@code @TableLogic} 插件干扰。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "已删除水表月报表条目")
public class WaterDeletedMeterMonthlyVO {

    @Schema(description = "统计月份（yyyy-MM）", example = "2026-05")
    private String statMonth;

    @Schema(description = "水表ID", example = "1001")
    private Long meterId;

    @Schema(description = "商户ID", example = "2001")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "设备编号")
    private String deviceNo;

    @Schema(description = "月用水量（吨）", example = "156.80")
    private BigDecimal totalUsage;

    @Schema(description = "月费用（元）", example = "533.12")
    private BigDecimal totalFee;

    @Schema(description = "月内有读数记录的天数", example = "28")
    private Integer dailyCount;

    @Schema(description = "水表删除时间（取 update_time）")
    private LocalDate deletedTime;

    @Schema(description = "数据来源：READING-原始读数实时聚合，ARCHIVE-历史归档表")
    private String dataSource;
}
