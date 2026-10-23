package com.competition.energycontrollerplatform.dto;

import com.competition.energycontrollerplatform.common.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求体。
 * <p>
 * 严禁包含 userId 或 username 字段——当前操作人身份由后端从
 * {@code SecurityContextHolder} 中安全提取，杜绝水平越权。
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @Sensitive(Sensitive.SensitiveType.PASSWORD)
    @Schema(description = "原密码", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Sensitive(Sensitive.SensitiveType.PASSWORD)
    @Schema(description = "新密码（6-20位）", example = "newPass456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20位之间")
    private String newPassword;
}
