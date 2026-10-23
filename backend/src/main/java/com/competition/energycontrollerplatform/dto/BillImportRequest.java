package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量导入账单请求。
 * 支持 Excel 解析后的数据批量导入。
 */
@Data
@Schema(description = "批量导入账单请求")
public class BillImportRequest {

    @Schema(description = "导入的账单数据列表")
    private List<BillImportItem> bills;

    @Schema(description = "账单分类ID")
    private Long categoryId;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "表计ID")
    private Long meterId;

    /**
     * 单条导入条目。
     */
    @Data
    @Schema(description = "账单导入条目")
    public static class BillImportItem {

        @Schema(description = "表号/钢印号", example = "ST20230001")
        private String meterNo;

        @Schema(description = "商户编号", example = "CUST001")
        private String customerCode;

        @Schema(description = "商户名称", example = "XX公司")
        private String customerName;

        @Schema(description = "账单金额（元）", example = "1580.50")
        private java.math.BigDecimal billAmount;

        @Schema(description = "用量", example = "100.5")
        private java.math.BigDecimal consumption;

        @Schema(description = "单价（元/kWh）", example = "0.85")
        private java.math.BigDecimal unitPrice;

        @Schema(description = "备注")
        private String remark;
    }
}
