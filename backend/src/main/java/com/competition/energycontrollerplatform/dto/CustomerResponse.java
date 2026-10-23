package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户信息响应
 */
@Data
@Schema(description = "商户信息响应")
public class CustomerResponse {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户编号")
    private String customerCode;

    @Schema(description = "用户姓名/商户名")
    private String customerName;

    @Schema(description = "小区名称")
    private String areaName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "用户电话")
    private String phone;

    @Schema(description = "用户类型")
    private String customerType;

    @Schema(description = "用水性质/水价类型")
    private String useNature;

    @Schema(description = "建档时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createDate;

    @Schema(description = "建档类型")
    private String type;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "签名")
    private String sign;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;
}
