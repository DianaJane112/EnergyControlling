package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商户信息修改请求
 */
@Data
@Schema(description = "商户信息修改请求")
public class CustomerUpdateRequest {

    @Schema(description = "用户姓名/商户名", example = "电子科大1号商铺（已更名）")
    private String customerName;

    @Schema(description = "小区名称")
    private String areaName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "用户电话", example = "13800138001")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "用户类型")
    private String customerType;

    @Schema(description = "用水性质/水价类型")
    private String useNature;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "签名")
    private String sign;

    @Schema(description = "备注")
    private String remark;
}
