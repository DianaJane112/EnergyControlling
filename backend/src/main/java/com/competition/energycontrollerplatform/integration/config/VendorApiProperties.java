package com.competition.energycontrollerplatform.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三个厂商（积成/捷先/昊普）的连接配置。
 * 在 application.yml 中以 integration 前缀覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration")
public class VendorApiProperties {

    private Jicheng jicheng = new Jicheng();
    private Jiexian jiexian = new Jiexian();
    private Haopu haopu = new Haopu();

    @Data
    public static class Jicheng {
        private String baseUrl = "http://127.0.0.1:8080/ws/rsyw";
        /** 积成项目数据范围编码。 */
        private String sign = "";
        private int connectTimeout = 10_000;
        private int readTimeout = 30_000;
    }

    @Data
    public static class Jiexian {
        private String baseUrl = "http://127.0.0.1:30002/camrgo/interface";
        /** 厂家编号 */
        private String companyCode = "0001";
        private int connectTimeout = 10_000;
        private int readTimeout = 30_000;
        private boolean v5Enabled = false;
        private String v5BaseUrl = "https://v5.camrgo.com/api";
        private String v5Username1 = "";
        private String v5Password1 = "";
        private String v5Username2 = "";
        private String v5Password2 = "";
        private int v5PageSize = 50;
        private int v5LookbackDays = 2;
    }

    @Data
    public static class Haopu {
        /** 基础地址 */
        private String baseUrl = "https://your-haopu-api-host.com";
        /** 应用ID */
        private String appId = "your_haopu_app_id";
        /** 应用密钥（签名用） */
        private String appSecret = "your_haopu_app_secret";
        /** Webhook 签名密钥 */
        private String webhookSignature = "";
        private int connectTimeout = 10_000;
        private int readTimeout = 30_000;
        /** 新版中国抄表在线 V5 网页 API。旧版 PDF 接口可继续独立保留。 */
    }
}
