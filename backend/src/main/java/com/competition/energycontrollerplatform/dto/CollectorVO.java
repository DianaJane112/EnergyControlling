package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基站/集中器响应 VO。
 */
@Data
@Schema(description = "集中器信息")
public class CollectorVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联商户ID")
    private Long customerId;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "集中器编号")
    private String collCode;

    @Schema(description = "集中器名称")
    private String collName;

    @Schema(description = "所属厂商代码")
    private String factCode;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "区域代码")
    private String areaCode;

    @Schema(description = "备注")
    private String memo;

    @Schema(description = "公司签名")
    private String sign;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;
}
