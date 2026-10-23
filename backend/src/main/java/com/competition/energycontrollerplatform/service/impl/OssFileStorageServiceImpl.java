package com.competition.energycontrollerplatform.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.config.StorageProperties;
import com.competition.energycontrollerplatform.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 OSS 对象存储实现。
 * <p>
 * 对象 Key 格式：{@code yyyy/MM/dd/{uuid}.ext}（含日期前缀）。
 * OSS 不需要预先创建"目录"，Key 中的 / 就是逻辑分隔符。
 * <p>
 * Bean 名称固定为 {@code ossFileStorageService}。
 * <p>
 * <b>仅在 {@code storage.active=oss} 时创建，避免本地开发时因 OSS 凭证未配置导致启动失败。</b>
 */
@Slf4j
@Service("ossFileStorageService")
@ConditionalOnProperty(name = "storage.active", havingValue = "oss")
@RequiredArgsConstructor
public class OssFileStorageServiceImpl implements FileStorageService {

    private final StorageProperties storageProperties;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        if (cfg.getAccessKeyId() == null || cfg.getAccessKeyId().isBlank()) {
            log.warn("[OSS存储] AccessKeyId 未配置，OSS 服务将不可用。请在 application.yml 中配置 storage.oss");
            return;
        }
        this.ossClient = new OSSClientBuilder().build(
                cfg.getEndpoint(), cfg.getAccessKeyId(), cfg.getAccessKeySecret());
        log.info("[OSS存储] 客户端初始化完成, endpoint={}, bucket={}",
                cfg.getEndpoint(), cfg.getBucketName());
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("[OSS存储] 客户端已关闭");
        }
    }

    @Override
    public String upload(String storedName, InputStream inputStream, String contentType, long fileSize) {
        ensureClient();
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        ossClient.putObject(cfg.getBucketName(), storedName, inputStream);
        String accessUrl = buildAccessUrl(storedName);
        log.info("[OSS存储] 上传成功: {} ({} bytes)", storedName, fileSize);
        return accessUrl;
    }

    @Override
    public InputStream download(String storedPath) {
        ensureClient();
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        OSSObject object = ossClient.getObject(cfg.getBucketName(), storedPath);
        return object.getObjectContent();
    }

    @Override
    public void delete(String storedPath) {
        ensureClient();
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        ossClient.deleteObject(cfg.getBucketName(), storedPath);
        log.info("[OSS存储] 已删除: {}", storedPath);
    }

    @Override
    public String getAccessUrl(String storedPath) {
        return buildAccessUrl(storedPath);
    }

    @Override
    public boolean exists(String storedPath) {
        ensureClient();
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        return ossClient.doesObjectExist(cfg.getBucketName(), storedPath);
    }

    /**
     * 列出 OSS 中的所有对象 Key（用于孤儿文件清理）。
     */
    public List<String> listAllObjectKeys() {
        ensureClient();
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        List<String> keys = new ArrayList<>();
        String nextMarker = null;
        ObjectListing listing;
        do {
            listing = ossClient.listObjects(
                    new com.aliyun.oss.model.ListObjectsRequest(cfg.getBucketName())
                            .withMarker(nextMarker));
            for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                keys.add(summary.getKey());
            }
            nextMarker = listing.getNextMarker();
        } while (listing.isTruncated());
        return keys;
    }

    // ──────── 私有方法 ────────

    private String buildAccessUrl(String storedPath) {
        StorageProperties.OssConfig cfg = storageProperties.getOss();
        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
            return cfg.getBaseUrl().replaceAll("/$", "") + "/" + storedPath;
        }
        return "https://" + cfg.getBucketName() + "." + cfg.getEndpoint() + "/" + storedPath;
    }

    private void ensureClient() {
        if (ossClient == null) {
            throw BusinessException.of(500, "OSS 存储服务未初始化，请检查 storage.oss 配置");
        }
    }
}
