package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商户开户请求
 */
@Data
@Schema(description = "商户开户请求")
public class CustomerCreateRequest {

    @Schema(description = "用户编号（营收系统唯一编号）", example = "KH20260001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户编号不能为空")
    private String customerCode;

    @Schema(description = "用户姓名/商户名", example = "电子科大1号商铺", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商户名不能为空")
    private String customerName;

    @Schema(description = "小区名称", example = "电子科大清水河校区")
    private String areaName;

    @Schema(description = "地址", example = "成都市高新西区西源大道2006号")
    private String address;

    @Schema(description = "用户电话", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "用户类型", example = "商业用户")
    private String customerType;

    @Schema(description = "用水性质/水价类型", example = "商业用水")
    private String useNature;

    @Schema(description = "建档类型：0建档/1变更/2过户/3换表", example = "0")
    private String type;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "签名")
    private String sign;

    @Schema(description = "备注")
    private String remark;
}
