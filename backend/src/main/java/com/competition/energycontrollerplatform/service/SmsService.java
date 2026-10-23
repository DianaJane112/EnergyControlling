package com.competition.energycontrollerplatform.service;

/**
 * 短信发送服务。
 * <p>
 * 开发模式（miniapp.sms.dev-mode=true）时使用固定验证码 000000，
 * 生产模式时调用真实短信平台（阿里云短信 / 腾讯云短信）。
 */
public interface SmsService {

    /**
     * 发送短信验证码。
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景描述（如"重置密码"），用于日志和短信模板选择
     */
    void sendVerificationCode(String phone, String code, String scene);
}
