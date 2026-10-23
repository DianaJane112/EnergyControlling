package com.competition.energycontrollerplatform.integration.wechat;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.SensitiveDataMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信 API 客户端。
 * <p>
 * 封装微信小程序 code2Session 接口调用。
 * 开发阶段使用 Mock 实现，生产环境配置真实 appid/secret 后自动切换。
 */
@Slf4j
@Component
public class WechatApiClient {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${miniapp.wechat.appid}")
    private String appid;

    @Value("${miniapp.wechat.secret}")
    private String secret;

    @Value("${miniapp.wechat.mock-enabled:false}")
    private boolean mockEnabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 微信 code2Session — 用登录凭证 code 换取 openid 和 session_key。
     * <p>
     * 开发环境（appid 为空或为占位符）：返回 Mock 数据。
     * 生产环境：调用微信官方 API。
     *
     * @param code 微信登录凭证（wx.login() 返回的 code）
     * @return WechatSession 包含 openid, session_key, unionid（可选）
     */
    public WechatSession code2Session(String code) {
        // 开发模式：appid 为占位符时使用 Mock
        if (mockEnabled) {
            log.info("微信 API Mock 模式: code2Session code={}", code);
            return mockCode2Session(code);
        }
        if (appid == null || appid.contains("placeholder") || appid.isBlank()
                || secret == null || secret.contains("placeholder") || secret.isBlank()) {
            throw new BusinessException("微信小程序 AppID/AppSecret 未配置，请联系管理员");
        }

        // 生产模式：真实调用微信 API
        try {
            String url = String.format(CODE2SESSION_URL, appid, secret, code);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                String errmsg = json.has("errmsg") ? json.get("errmsg").asText() : "未知错误";
                log.error("微信 code2Session 失败: errcode={}, errmsg={}", json.get("errcode").asInt(), errmsg);
                throw new BusinessException("微信登录失败，请稍后重试");
            }

            WechatSession session = new WechatSession();
            session.setOpenid(json.get("openid").asText());
            session.setSessionKey(json.has("session_key") ? json.get("session_key").asText() : null);
            session.setUnionid(json.has("unionid") ? json.get("unionid").asText() : null);

            log.info("微信 code2Session 成功: openid={}", SensitiveDataMasker.maskOpenId(session.getOpenid()));
            return session;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 code2Session 调用异常", e);
            throw new BusinessException("微信登录服务暂不可用，请稍后重试");
        }
    }

    /**
     * Mock 实现（开发阶段使用）。
     * 将 code 直接映射为 openid，方便前后端联调测试。
     */
    private WechatSession mockCode2Session(String code) {
        WechatSession session = new WechatSession();
        session.setOpenid("wx_openid_" + code);
        session.setSessionKey("mock_session_key_" + code);
        session.setUnionid(null);
        return session;
    }

    /**
     * 微信 code2Session 返回的会话信息。
     */
    @lombok.Data
    public static class WechatSession {
        private String openid;
        private String sessionKey;
        private String unionid;
    }
}
