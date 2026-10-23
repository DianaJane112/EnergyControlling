package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.config.StorageProperties;
import com.competition.energycontrollerplatform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

/**
 * 本地磁盘文件存储实现。
 * <p>
 * 文件存储在服务器本地文件系统中，路径格式：
 * {@code {rootPath}/yyyy/MM/dd/{uuid}.ext}
 * <p>
 * Bean 名称固定为 {@code localFileStorageService}，
 * 供 {@code FileStorageFactory} 按名称查找。
 */
@Slf4j
@Service("localFileStorageService")
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final StorageProperties storageProperties;

    @Override
    public String upload(String storedName, InputStream inputStream, String contentType, long fileSize) {
        Path rootPath = resolveRootPath();
        Path targetPath = rootPath.resolve(storedName);
        try {
            // 确保父目录存在（如 ./uploads/2026/06/17/）
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[本地存储] 上传成功: {} ({} bytes)", storedName, fileSize);
            return null; // URL 由 Controller 动态拼接
        } catch (IOException e) {
            throw new BusinessException(500, "本地文件存储失败: " + storedName);
        }
    }

    @Override
    public InputStream download(String storedPath) {
        Path rootPath = resolveRootPath();
        Path filePath = rootPath.resolve(storedPath);
        if (!Files.exists(filePath)) {
            throw BusinessException.of(404, "文件不存在: " + storedPath);
        }
        try {
            return new FileInputStream(filePath.toFile());
        } catch (FileNotFoundException e) {
            throw BusinessException.of(404, "文件不存在: " + storedPath);
        }
    }

    @Override
    public void delete(String storedPath) {
        Path rootPath = resolveRootPath();
        Path filePath = rootPath.resolve(storedPath);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[本地存储] 已删除: {}", storedPath);
                // 尝试清理空目录（向上追溯直到 rootPath）
                Path parent = filePath.getParent();
                while (parent != null && !parent.equals(rootPath)) {
                    try {
                        Files.deleteIfExists(parent);
                    } catch (IOException ignored) {
                        // 目录非空，停止追溯
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        } catch (IOException e) {
            log.warn("[本地存储] 删除失败: {}", storedPath, e);
        }
    }

    @Override
    public String getAccessUrl(String storedPath) {
        return null; // 本地 URL 由 Controller 动态拼接
    }

    @Override
    public boolean exists(String storedPath) {
        Path rootPath = resolveRootPath();
        return Files.exists(rootPath.resolve(storedPath));
    }

    /**
     * 列出本地存储中的所有文件路径（用于孤儿文件清理）。
     *
     * @return 所有已存储文件的相对路径流
     */
    public java.util.stream.Stream<Path> listAllFiles() throws IOException {
        Path rootPath = resolveRootPath();
        if (!Files.exists(rootPath)) {
            return java.util.stream.Stream.empty();
        }
        return Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .map(rootPath::relativize);
    }

    // ──────── 私有方法 ────────

    private Path resolveRootPath() {
        String path = storageProperties.getLocal().getRootPath();
        Path rootPath = Paths.get(path);
        if (!rootPath.isAbsolute()) {
            rootPath = Paths.get("").toAbsolutePath().resolve(path);
        }
        return rootPath.normalize();
    }
}
