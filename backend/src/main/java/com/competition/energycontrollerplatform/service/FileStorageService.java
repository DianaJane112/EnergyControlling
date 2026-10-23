package com.competition.energycontrollerplatform.service;

import java.io.InputStream;

/**
 * 文件存储后端策略接口（Strategy Pattern）。
 * <p>
 * 所有存储后端（本地磁盘、阿里云 OSS、MinIO、AWS S3 等）
 * 必须实现此接口，由 {@link FileStorageFactory} 按配置激活。
 * <p>
 * <b>日期前缀约定：</b>所有实现类的 {@code upload} 方法必须在
 * {@code storedName} 前拼接 {@code yyyy/MM/dd/} 日期前缀，
 * 避免单目录文件数过多导致文件系统性能下降。
 */
public interface FileStorageService {

    /**
     * 上传文件到存储后端。
     *
     * @param storedName  已拼接日期前缀的唯一存储文件名（如 "2026/06/17/a1b2c3.jpg"）
     * @param inputStream 文件内容流（调用方负责关闭）
     * @param contentType 真实 MIME 类型（通过魔数检测得出）
     * @param fileSize    文件大小（字节）
     * @return 外部访问 URL（本地存储返回 null，由 Controller 动态拼接）
     */
    String upload(String storedName, InputStream inputStream, String contentType, long fileSize);

    /**
     * 下载文件。
     *
     * @param storedPath 存储路径/对象 Key（含日期前缀）
     * @return 文件内容流（调用方负责关闭）
     */
    InputStream download(String storedPath);

    /**
     * 从存储后端删除文件。
     *
     * @param storedPath 存储路径/对象 Key（含日期前缀）
     */
    void delete(String storedPath);

    /**
     * 生成外部访问 URL。
     * <ul>
     *   <li>云存储（OSS/MinIO）：返回完整的外网可访问 URL</li>
     *   <li>本地存储：返回 null，由 Controller 层通过 {@code ServletUriComponentsBuilder} 动态拼接</li>
     * </ul>
     *
     * @param storedPath 存储路径/对象 Key（含日期前缀）
     * @return 完整访问 URL，本地存储返回 null
     */
    String getAccessUrl(String storedPath);

    /**
     * 检查文件是否存在于存储后端。
     *
     * @param storedPath 存储路径/对象 Key（含日期前缀）
     * @return true 如果文件存在
     */
    boolean exists(String storedPath);
}
