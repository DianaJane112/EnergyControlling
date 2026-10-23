package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 银行账户请求 DTO（新增/编辑共用）。
 */
@Data
@Schema(description = "银行账户请求")
public class BankAccountRequest {

    @Schema(description = "银行名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国工商银行")
    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @Schema(description = "账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX物业管理有限公司")
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    @Schema(description = "银行账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6222021234567890123")
    @NotBlank(message = "银行账号不能为空")
    private String accountNumber;

    @Schema(description = "开户支行", example = "XX支行")
    private String bankBranch;

    @Schema(description = "开户行联行号", example = "102100000001")
    private String bankCode;

    @Schema(description = "账户类型：COMPANY-对公账户，PERSONAL-个人账户", example = "COMPANY")
    private String accountType;

    @Schema(description = "是否默认账户：0-否，1-是", example = "0")
    private Integer isDefault;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800000000")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：0-停用，1-启用", example = "1")
    private Integer status;
}
