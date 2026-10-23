package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 商户分页查询请求
 */
@Data
@Schema(description = "商户分页查询请求")
public class CustomerPageRequest {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    @Schema(description = "商户名称（模糊查询）", example = "电子科大")
    private String customerName;

    @Schema(description = "用户编号")
    private String customerCode;

    @Schema(description = "小区名称")
    private String areaName;

    @Schema(description = "用户类型")
    private String customerType;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    /** 业务表计类型：0=水表（捷先/积成），1=电表（昊普） */
    private Integer meterType;
}
