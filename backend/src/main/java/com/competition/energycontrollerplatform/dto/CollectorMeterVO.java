package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基站下绑定的电表/水表信息 VO。
 */
@Data
@Schema(description = "基站下电表/水表信息")
public class CollectorMeterVO {

    @Schema(description = "表计ID")
    private Long id;

    @Schema(description = "表钢印号（积成）")
    private String stealNo;

    @Schema(description = "电表表号（辰鳗）")
    private String dbNo;

    @Schema(description = "水表编号（捷先）")
    private String meteraddress;

    @Schema(description = "表计名称")
    private String name;

    @Schema(description = "表计类型：0=水表, 1=电表")
    private Integer meterType;

    @Schema(description = "表计类型名称")
    private String meterTypeName;

    @Schema(description = "运行状态：0-停用, 1-正常运行, 2-欠费停机")
    private Integer status;

    @Schema(description = "在线状态：0-离线, 1-在线")
    private Integer online;

    @Schema(description = "集中器编号")
    private String collectorCode;

    @Schema(description = "集中器设备号")
    private String collectorDeviceNo;

    @Schema(description = "商户ID")
    private Long customerId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
