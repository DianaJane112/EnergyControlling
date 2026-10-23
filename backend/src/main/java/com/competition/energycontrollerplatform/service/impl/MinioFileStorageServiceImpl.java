package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.config.StorageProperties;
import com.competition.energycontrollerplatform.service.FileStorageService;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 对象存储实现（S3 兼容）。
 * <p>
 * 对象 Key 格式：{@code yyyy/MM/dd/{uuid}.ext}（含日期前缀）。
 * 启动时自动检测并创建 Bucket（如果不存在）。
 * <p>
 * Bean 名称固定为 {@code minioFileStorageService}。
 * <p>
 * <b>仅在 {@code storage.active=minio} 时创建，避免本地开发时因 MinIO 未安装导致启动失败。</b>
 */
@Slf4j
@Service("minioFileStorageService")
@ConditionalOnProperty(name = "storage.active", havingValue = "minio")
@RequiredArgsConstructor
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final StorageProperties storageProperties;
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        if (cfg.getEndpoint() == null || cfg.getEndpoint().isBlank()) {
            log.warn("[MinIO存储] Endpoint 未配置，MinIO 服务将不可用");
            return;
        }
        this.minioClient = MinioClient.builder()
                .endpoint(cfg.getEndpoint())
                .credentials(cfg.getAccessKey(), cfg.getSecretKey())
                .build();
        ensureBucketExists(cfg.getBucketName());
        log.info("[MinIO存储] 客户端初始化完成, endpoint={}, bucket={}",
                cfg.getEndpoint(), cfg.getBucketName());
    }

    @Override
    public String upload(String storedName, InputStream inputStream, String contentType, long fileSize) {
        ensureClient();
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(cfg.getBucketName())
                    .object(storedName)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build());
            String accessUrl = buildAccessUrl(storedName);
            log.info("[MinIO存储] 上传成功: {} ({} bytes)", storedName, fileSize);
            return accessUrl;
        } catch (Exception e) {
            throw new BusinessException(500, "MinIO 文件上传失败: " + storedName);
        }
    }

    @Override
    public InputStream download(String storedPath) {
        ensureClient();
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(cfg.getBucketName())
                    .object(storedPath)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(404, "MinIO 文件下载失败: " + storedPath);
        }
    }

    @Override
    public void delete(String storedPath) {
        ensureClient();
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(cfg.getBucketName())
                    .object(storedPath)
                    .build());
            log.info("[MinIO存储] 已删除: {}", storedPath);
        } catch (Exception e) {
            log.warn("[MinIO存储] 删除失败: {}", storedPath, e);
        }
    }

    @Override
    public String getAccessUrl(String storedPath) {
        return buildAccessUrl(storedPath);
    }

    @Override
    public boolean exists(String storedPath) {
        ensureClient();
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(cfg.getBucketName())
                    .object(storedPath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 列出 MinIO 中的所有对象 Key（用于孤儿文件清理）。
     */
    public List<String> listAllObjectKeys() {
        ensureClient();
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        List<String> keys = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(cfg.getBucketName()).recursive(true).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    keys.add(item.objectName());
                }
            }
        } catch (Exception e) {
            log.error("[MinIO存储] 列举对象失败", e);
        }
        return keys;
    }

    // ──────── 私有方法 ────────

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("[MinIO存储] 桶已自动创建: {}", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 桶初始化失败: " + bucketName, e);
        }
    }

    private String buildAccessUrl(String storedPath) {
        StorageProperties.MinioConfig cfg = storageProperties.getMinio();
        return cfg.getEndpoint().replaceAll("/$", "") + "/" + cfg.getBucketName() + "/" + storedPath;
    }

    private void ensureClient() {
        if (minioClient == null) {
            throw BusinessException.of(500, "MinIO 存储服务未初始化，请检查 storage.minio 配置");
        }
    }
}
