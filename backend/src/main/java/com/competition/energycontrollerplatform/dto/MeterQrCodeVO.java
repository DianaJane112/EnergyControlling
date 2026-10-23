package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表计二维码信息响应（管理后台）。
 * <p>
 * 管理端生成二维码前先调用此接口获取二维码内容和表计摘要，
 * 前端可据此展示预览或调用下载接口获取 PNG 图片。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "表计二维码信息")
public class MeterQrCodeVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号（dbNo）", example = "010000000001")
    private String dbNo;

    @Schema(description = "表计名称")
    private String meterName;

    @Schema(description = "表计类型: 0=水表, 1=电表")
    private Integer meterType;

    @Schema(description = "表计类型中文名", example = "电表")
    private String meterTypeName;

    @Schema(description = "当前绑定的商户名称（未绑定时为空）")
    private String customerName;

    @Schema(description = "二维码内容字符串（扫码后小程序解析的原文）", example = "METER:1:010000000001")
    private String qrContent;
}
