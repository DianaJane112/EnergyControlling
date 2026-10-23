package com.competition.energycontrollerplatform.common.enums;

import lombok.Getter;

/**
 * 文件存储类型枚举。
 * <p>
 * 对应 {@code storage.active} 配置项，控制文件实际存储的目标后端。
 * <p>
 * 扩展新存储类型时只需新增枚举值 + 对应的 FileStorageService 实现类。
 */
@Getter
public enum StorageType {

    /** 本地磁盘存储（开发/单机部署） */
    LOCAL("local", "本地磁盘"),

    /** 阿里云对象存储 OSS */
    OSS("oss", "阿里云OSS"),

    /** MinIO 对象存储（S3 兼容、私有化部署） */
    MINIO("minio", "MinIO"),

    /** AWS S3（预留） */
    S3("s3", "AWS S3"),

    /** 腾讯云 COS（预留） */
    COS("cos", "腾讯云COS"),

    /** 华为云 OBS（预留） */
    OBS("obs", "华为云OBS");

    private final String code;
    private final String label;

    StorageType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据配置编码获取枚举。
     *
     * @param code 配置中的存储类型编码
     * @return 对应的枚举值，未知编码默认返回 LOCAL
     */
    public static StorageType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return LOCAL;
        }
        for (StorageType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("不支持的存储类型: " + code
                + "，可用值: local / oss / minio / s3 / cos / obs");
    }
}
