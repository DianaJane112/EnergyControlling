package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.FileUploadResult;
import com.competition.energycontrollerplatform.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件管理服务接口（高层业务编排）。
 * <p>
 * 负责文件上传的完整流程：安全校验 → 生成存储名 → 委托存储后端 → 持久化元数据。
 * 与底层 {@link FileStorageService} 的区别在于本层处理了安全策略（魔数检测、类型白名单）
 * 和数据库元数据管理。
 */
public interface FileService {

    /**
     * 上传文件（REST 接口使用）。
     *
     * @param file   MultipartFile 上传对象
     * @param module 业务模块标识（report / QR / bill / contract / meter-photo）
     * @param refId  关联业务记录 ID（可为 null）
     * @return 上传结果（含文件 ID、访问 URL 等）
     */
    FileUploadResult upload(MultipartFile file, String module, Long refId);

    /**
     * 上传文件（程序化调用）。
     * <p>
     * 用于 QR 码生成、Excel 导出等内存产出的持久化场景。
     *
     * @param inputStream  文件内容流
     * @param originalName 原始文件名（用于提取扩展名）
     * @param contentType  真实 MIME 类型（调用方已知，跳过魔数检测）
     * @param fileSize     文件大小（字节）
     * @param module       业务模块
     * @param refId        关联业务 ID
     * @param autoLink     是否自动标记 isLinked=1
     * @return 上传结果
     */
    FileUploadResult upload(InputStream inputStream, String originalName, String contentType,
                            long fileSize, String module, Long refId, boolean autoLink);

    /**
     * 根据 ID 获取文件元数据。
     */
    FileRecord getById(Long id);

    /**
     * 根据存储名获取文件元数据。
     */
    FileRecord getByStoredName(String storedName);

    /**
     * 下载文件内容。
     *
     * @param id 文件记录 ID
     * @return 文件内容流（调用方负责关闭）
     */
    InputStream download(Long id);

    /**
     * 获取文件外部访问 URL。
     */
    String getAccessUrl(Long id);

    /**
     * 删除文件（物理删除存储 + 逻辑删除数据库记录）。
     */
    void delete(Long id);

    /**
     * 分页查询文件记录。
     */
    IPage<FileRecord> listPage(String module, Long refId, int pageNum, int pageSize);

    /**
     * 将文件标记为已关联业务（isLinked = 1）。
     * <p>
     * 业务层在完成数据绑定后调用，保护文件不被孤儿清理任务误删。
     */
    void markLinked(Long id);
}
