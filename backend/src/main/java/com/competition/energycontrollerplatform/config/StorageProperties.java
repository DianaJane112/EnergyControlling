package com.competition.energycontrollerplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储统一配置属性。
 * <p>
 * 遵循 {@link VendorApiProperties} 的模式：通过 {@code @ConfigurationProperties}
 * 将 application.yml 中以 {@code storage} 为前缀的配置绑定到此 POJO。
 * <p>
 * 各子配置类提供合理的开发环境默认值；生产环境通过环境变量覆盖。
 *
 * <pre>{@code
 * storage:
 *   active: local
 *   local:
 *     root-path: ./uploads
 *   oss:
 *     endpoint: oss-cn-hangzhou.aliyuncs.com
 *     access-key-id: ...
 *     access-key-secret: ...
 *     bucket-name: energy-platform
 *   minio:
 *     endpoint: http://localhost:9000
 *     access-key: minioadmin
 *     secret-key: minioadmin
 *     bucket-name: energy-platform
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 当前激活的存储后端（local / oss / minio），默认 local */
    private String active = "local";

    /** 服务端基础 URL，用于生成本地文件的完整外部访问地址 */
    private String baseUrl = "http://localhost:8080";

    /** 单文件最大大小（字节），默认 50MB */
    private long maxFileSize = 50 * 1024 * 1024;

    /** 允许上传的文件 MIME 类型白名单（为空则不限制类型） */
    private String[] allowedMimeTypes = {};

    /** 允许上传的文件扩展名白名单（为空则不限制扩展名） */
    private String[] allowedExtensions = {};

    private LocalConfig local = new LocalConfig();
    private OssConfig oss = new OssConfig();
    private MinioConfig minio = new MinioConfig();

    // ────────────── 子配置类 ──────────────

    @Data
    public static class LocalConfig {
        /** 本地文件存储根目录（相对路径相对于工作目录，绝对路径直接使用） */
        private String rootPath = "./uploads";
        /** 通过 HTTP 访问本地文件的 URL 前缀（用于静态资源映射） */
        private String urlPrefix = "/files";
    }

    @Data
    public static class OssConfig {
        /** OSS endpoint（地域节点），如 oss-cn-hangzhou.aliyuncs.com */
        private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        /** AccessKey ID */
        private String accessKeyId;
        /** AccessKey Secret */
        private String accessKeySecret;
        /** OSS 桶名称 */
        private String bucketName = "energy-platform";
        /** 外部访问基础 URL（可配置 CDN 域名），不填则使用默认 OSS 域名 */
        private String baseUrl;
    }

    @Data
    public static class MinioConfig {
        /** MinIO 服务端点（含协议和端口），如 http://localhost:9000 */
        private String endpoint = "http://localhost:9000";
        /** 访问密钥 */
        private String accessKey = "minioadmin";
        /** 秘密密钥 */
        private String secretKey = "minioadmin";
        /** 桶名称 */
        private String bucketName = "energy-platform";
        /** 是否使用路径风格访问（MinIO 推荐 true） */
        private boolean pathStyleAccess = true;
    }
}
