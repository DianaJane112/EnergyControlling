package com.competition.energycontrollerplatform.miniapp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.energycontrollerplatform.miniapp.dto.BindCustomerRequest;
import com.competition.energycontrollerplatform.miniapp.dto.MiniAppLoginVO;
import com.competition.energycontrollerplatform.miniapp.dto.PhoneLoginRequest;
import com.competition.energycontrollerplatform.miniapp.dto.WechatLoginRequest;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;

/**
 * 小程序用户服务接口
 */
public interface MiniAppUserService extends IService<MiniAppUser> {

    /**
     * 微信登录。
     * 根据微信 code 换取 openid，查找或提示绑定商户。
     *
     * @param request 微信登录请求（含 code）
     * @return 登录响应（可能 needBindCustomer=true）
     */
    MiniAppLoginVO wechatLogin(WechatLoginRequest request);

    /**
     * 手机号登录。
     * 验证短信验证码 → 查找或自动注册用户 → 绑定商户 → 生成 Token。
     *
     * @param request 手机号登录请求（含手机号和验证码）
     * @return 登录响应
     */
    MiniAppLoginVO phoneLogin(PhoneLoginRequest request);

    /**
     * 绑定商户 — 微信新用户通过手机号验证码绑定已有商户。
     * <p>
     * 流程：验证短信码 → 匹配手机号与商户电话 → 创建/更新 MiniAppUser → 生成 Token。
     *
     * @param request 绑定请求（含 openid、手机号、验证码）
     * @return 登录响应（含 token 和商户信息）
     */
    MiniAppLoginVO bindCustomer(BindCustomerRequest request);

    /**
     * 获取当前用户信息（含商户和表计列表）。
     *
     * @param userId 小程序用户ID
     * @return 登录响应（含完整用户和商户信息）
     */
    MiniAppLoginVO getCurrentUserInfo(Long userId);

    /**
     * 递增用户的 tokenVersion，使所有已签发的 Token 即时报废。
     * 用于登出场景。
     */
    void incrementTokenVersion(Long userId);
}
