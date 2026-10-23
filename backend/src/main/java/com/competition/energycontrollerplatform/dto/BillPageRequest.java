package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账单记录分页查询请求。
 */
@Data
@Schema(description = "账单记录分页查询请求")
public class BillPageRequest {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize;

    @Schema(description = "商户名（模糊搜索）")
    private String merchantName;

    @Schema(description = "表号（模糊搜索）")
    private String meterNo;

    @Schema(description = "虚拟表商户名（模糊搜索）")
    private String virtualMerchantName;

    @Schema(description = "虚拟表号（模糊搜索）")
    private String virtualMeterNo;

    @Schema(description = "账单分类ID")
    private Long categoryId;

    @Schema(description = "支付状态")
    private String paymentStatus;

    @Schema(description = "账单生成时间起")
    private String billCreateStart;

    @Schema(description = "账单生成时间止")
    private String billCreateEnd;

    @Schema(description = "实际支付时间起")
    private String payStart;

    @Schema(description = "实际支付时间止")
    private String payEnd;
}
