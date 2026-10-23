package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 基站/集中器请求 DTO。
 */
@Data
@Schema(description = "集中器新增/编辑请求")
public class CollectorRequest {

    @Schema(description = "关联商户ID（非ADMIN强制覆盖）")
    private Long customerId;

    @NotBlank(message = "集中器编号不能为空")
    @Schema(description = "集中器编号（积成GetTerminal）", example = "COLL-001")
    private String collCode;

    @Schema(description = "集中器名称", example = "翠苑一区1号集中器")
    private String collName;

    @Schema(description = "所属厂商代码", example = "JC001")
    private String factCode;

    @Schema(description = "安装位置", example = "翠苑一区配电房A")
    private String location;

    @Schema(description = "区域代码", example = "AREA-001")
    private String areaCode;

    @Schema(description = "备注")
    private String memo;

    @Schema(description = "公司签名")
    private String sign;
}
