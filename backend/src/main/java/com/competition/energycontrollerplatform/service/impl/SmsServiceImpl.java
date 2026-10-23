package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.SensitiveDataMasker;
import com.competition.energycontrollerplatform.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 短信发送服务实现。
 * <p>
 * 开发模式（miniapp.sms.dev-mode=true）：日志打印验证码，不实际发送。
 * 生产模式（miniapp.sms.dev-mode=false）：需要甲方配置短信平台凭证，
 * 未配置时抛出明确的配置异常，避免静默失败。
 */
@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Value("${miniapp.sms.dev-mode:true}")
    private boolean devMode;

    @Value("${miniapp.sms.dev-code:000000}")
    private String devCode;

    @Value("${miniapp.sms.provider:}")
    private String smsProvider;

    @Value("${miniapp.sms.access-key:}")
    private String accessKey;

    @Value("${miniapp.sms.secret-key:}")
    private String secretKey;

    @Override
    public void sendVerificationCode(String phone, String code, String scene) {
        if (devMode) {
            log.info("[短信-开发模式] 场景={}, 手机号={}", scene, SensitiveDataMasker.maskPhone(phone));
            return;
        }

        // ── 生产模式 ──────────────────────────────────────────────
        // 检查短信平台是否已配置
        if (smsProvider == null || smsProvider.isBlank()) {
            log.error("[短信] 生产模式未配置短信服务商，请在 application-prod.yml 中配置 miniapp.sms.* 参数");
            throw BusinessException.of(500, "短信服务未配置，请联系系统管理员");
        }

        log.info("[短信-生产模式] 场景={}, 手机号={}, 服务商={}", scene, SensitiveDataMasker.maskPhone(phone), smsProvider);

        // ── 对接真实短信平台 ─────────────────────────────────────
        // 示例（阿里云短信）：
        //   SendSmsRequest req = new SendSmsRequest();
        //   req.setPhoneNumbers(phone);
        //   req.setSignName("小鳗管家");
        //   req.setTemplateCode(templateId);
        //   req.setTemplateParam("{\"code\":\"" + code + "\"}");
        //   client.sendSms(req);
        //
        // 示例（腾讯云短信）：
        //   SmsSingleSender sender = new SmsSingleSender(appId, appKey);
        //   sender.sendWithParam("86", phone, templateId,
        //       new String[]{code}, signName, "", "");

        throw BusinessException.of(500,
                "短信服务商 [" + smsProvider + "] 尚未实现对接，请联系系统管理员配置 SDK");
    }
}
