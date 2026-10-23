package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.config.StorageProperties;
import com.competition.energycontrollerplatform.dto.FileUploadResult;
import com.competition.energycontrollerplatform.entity.FileRecord;
import com.competition.energycontrollerplatform.mapper.FileRecordMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.FileService;
import com.competition.energycontrollerplatform.service.FileStorageFactory;
import com.competition.energycontrollerplatform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件管理服务实现。
 * <p>
 * <b>安全加固：</b>
 * <ul>
 *   <li>通过 Apache Tika 检测文件魔数（Magic Number），获取真实 MIME 类型，
 *       而非信任用户声称的文件名后缀。防止脚本伪装成图片上传。</li>
 *   <li>支持配置文件类型白名单（allowedMimeTypes / allowedExtensions）</li>
 * </ul>
 * <p>
 * <b>日期前缀路径：</b>
 * <ul>
 *   <li>所有文件以 {@code yyyy/MM/dd/uuid.ext} 格式存储，
 *       避免单目录文件数过多导致文件系统性能下降。</li>
 * </ul>
 * <p>
 * <b>孤儿文件防护：</b>
 * <ul>
 *   <li>默认 {@code isLinked = 0}，业务层完成数据绑定后调用 {@link #markLinked(Long)} 置为 1</li>
 *   <li>{@code OrphanFileCleanupTask} 定时清理 isLinked=0 且超过 24 小时的文件</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;
    private final FileStorageFactory storageFactory;
    private final StorageProperties storageProperties;

    /** Apache Tika 实例（线程安全，复用） */
    private static final Tika TIKA = new Tika();

    /** 日期前缀格式化器（yyyy/MM/dd） */
    private static final DateTimeFormatter DATE_PREFIX_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 默认允许的 MIME 类型（安全白名单） */
    private static final Set<String> DEFAULT_ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            // 图片
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml",
            // 文档
            "application/pdf",
            "application/msword",                                          // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel",                                    // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       // .xlsx
            "application/vnd.ms-powerpoint",                               // .ppt
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            // 文本/数据
            "text/plain", "text/csv", "application/json", "application/xml", "text/xml",
            // 压缩包
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/gzip"
    ));

    /** 默认允许的文件扩展名（安全白名单） */
    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".json", ".xml",
            ".zip", ".rar", ".7z", ".gz"
    ));

    // ──────────────────────── REST 上传 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult upload(MultipartFile file, String module, Long refId) {
        // 1. 基础校验
        if (file.isEmpty()) {
            throw BusinessException.of(400, "上传文件不能为空");
        }

        // 2. 通过魔数检测真实 MIME 类型（安全加固核心）
        String detectedMimeType;
        try {
            // Tika.detect() 只读取文件头部几个字节的魔数，不会将整个文件加载到内存
            detectedMimeType = TIKA.detect(file.getInputStream());
        } catch (IOException e) {
            throw BusinessException.of(400, "无法读取文件内容，请确认文件未损坏");
        }
        log.debug("[文件上传] 魔数检测: 声称类型={}, 真实类型={}, 文件名={}",
                file.getContentType(), detectedMimeType, file.getOriginalFilename());

        // 3. 类型校验（白名单）
        validateFileType(detectedMimeType, file.getOriginalFilename());

        // 4. 委托底层上传
        return doUpload(file.getOriginalFilename(), detectedMimeType, file.getSize(),
                module, refId, false, outputStream -> {
                    try {
                        file.getInputStream().transferTo(outputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    // ──────────────────────── 程序化上传 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult upload(InputStream inputStream, String originalName,
                                   String contentType, long fileSize,
                                   String module, Long refId, boolean autoLink) {
        if (originalName == null || originalName.isBlank()) {
            throw BusinessException.of(400, "文件名不能为空");
        }
        if (fileSize <= 0) {
            throw BusinessException.of(400, "文件大小不能为0");
        }

        // 程序化上传时信任调用方提供的 contentType（由 QrCodeGenerator 等内部工具产出）
        // 但如果是外部输入，调用方应自行检测
        return doUpload(originalName, contentType, fileSize, module, refId, autoLink,
                outputStream -> {
                    try {
                        inputStream.transferTo(outputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    // ──────────────────────── 核心上传逻辑 ────────────────────────

    /**
     * 上传核心流程：校验 → 生成存储名（含日期前缀） → 写入存储 → 持久化元数据。
     * <p>
     * 使用 {@link ByteArrayOutputStream} 缓冲实现"先写入存储，再写 DB"的流程。
     * 如果存储写入成功但 DB 插入失败，文件成为"孤儿"——由 {@code OrphanFileCleanupTask} 定时清理。
     */
    private FileUploadResult doUpload(String originalName, String mimeType, long fileSize,
                                      String module, Long refId, boolean autoLink,
                                      StreamWriter streamWriter) {
        // 1. 文件大小校验
        if (fileSize > storageProperties.getMaxFileSize()) {
            long maxMb = storageProperties.getMaxFileSize() / 1024 / 1024;
            throw BusinessException.of(400, "文件大小超过限制（最大 " + maxMb + "MB）");
        }

        // 2. 生成存储名（日期前缀 + UUID + 扩展名）
        String extension = extractExtension(originalName);
        String datePrefix = LocalDate.now().format(DATE_PREFIX_FMT);
        String storedNameWithoutDate = UUID.randomUUID().toString().replace("-", "") + extension;
        String storedPath = datePrefix + "/" + storedNameWithoutDate;

        // 3. 写入存储后端（先于 DB，使用缓冲区避免大文件内存问题，但仍需流式处理）
        FileStorageService storage = storageFactory.getStorage();
        byte[] buffer;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            streamWriter.write(baos);
            buffer = baos.toByteArray();
        } catch (IOException | UncheckedIOException e) {
            throw new BusinessException(500, "文件读取失败: " + originalName);
        }

        String accessUrl;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buffer)) {
            accessUrl = storage.upload(storedPath, bais, mimeType, buffer.length);
        } catch (Exception e) {
            log.error("[文件上传] 存储写入失败: {}", storedPath, e);
            throw new BusinessException(500, "文件存储失败，请稍后重试");
        }

        // 4. 持久化元数据到数据库
        FileRecord record = new FileRecord();
        record.setOriginalName(originalName);
        record.setStoredName(storedNameWithoutDate);
        record.setStoredPath(storedPath);
        record.setAccessUrl(accessUrl);
        record.setFileSize((long) buffer.length);
        record.setMimeType(mimeType);
        record.setExtension(extension);
        record.setStorageType(storageProperties.getActive().toUpperCase());
        record.setUploaderId(SecurityUserContext.getCurrentUserId());
        record.setUploaderName(SecurityUserContext.getCurrentUsername());
        record.setModule(module);
        record.setRefId(refId);
        record.setIsLinked(autoLink ? 1 : 0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            fileRecordMapper.insert(record);
        } catch (Exception e) {
            // DB 插入失败 → 尝试回滚存储（尽力而为）
            log.error("[文件上传] 数据库写入失败，尝试删除已上传的存储对象: {}", storedPath, e);
            try {
                storage.delete(storedPath);
            } catch (Exception ex) {
                log.error("[文件上传] 回滚删除存储对象也失败了，文件成为孤儿: {}", storedPath, ex);
            }
            throw new BusinessException(500, "文件记录保存失败，请稍后重试");
        }

        log.info("[文件管理] 上传成功: id={}, originalName={}, storedPath={}, mimeType={}, module={}, linked={}",
                record.getId(), originalName, storedPath, mimeType, module, autoLink);

        return FileUploadResult.from(record);
    }

    // ──────────────────────── 查询 ────────────────────────

    @Override
    public FileRecord getById(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.of(404, "文件记录不存在");
        }
        return record;
    }

    @Override
    public FileRecord getByStoredName(String storedName) {
        FileRecord record = fileRecordMapper.selectOne(
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getStoredName, storedName));
        if (record == null) {
            throw BusinessException.of(404, "文件记录不存在");
        }
        return record;
    }

    // ──────────────────────── 下载 ────────────────────────

    @Override
    public InputStream download(Long id) {
        FileRecord record = getById(id);
        FileStorageService storage = storageFactory.getStorage(record.getStorageType().toLowerCase());
        return storage.download(record.getStoredPath());
    }

    // ──────────────────────── 访问 URL ────────────────────────

    @Override
    public String getAccessUrl(Long id) {
        FileRecord record = getById(id);
        if (record.isLocal()) {
            // 本地存储：返回相对路径，由 Controller 拼接完整 URL
            return null;
        }
        FileStorageService storage = storageFactory.getStorage(record.getStorageType().toLowerCase());
        return storage.getAccessUrl(record.getStoredPath());
    }

    // ──────────────────────── 删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileRecord record = getById(id);
        // 从物理存储中删除
        FileStorageService storage = storageFactory.getStorage(record.getStorageType().toLowerCase());
        storage.delete(record.getStoredPath());
        // 逻辑删除数据库记录
        fileRecordMapper.logicDeleteById(id);
        log.info("[文件管理] 已删除: id={}, storedPath={}", id, record.getStoredPath());
    }

    // ──────────────────────── 分页 ────────────────────────

    @Override
    public IPage<FileRecord> listPage(String module, Long refId, int pageNum, int pageSize) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(module), FileRecord::getModule, module)
               .eq(refId != null, FileRecord::getRefId, refId)
               .orderByDesc(FileRecord::getCreateTime);
        Page<FileRecord> page = new Page<>(pageNum, pageSize);
        return fileRecordMapper.selectPage(page, wrapper);
    }

    // ──────────────────────── 关联标记 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markLinked(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.of(404, "文件记录不存在");
        }
        if (!record.isLinked()) {
            FileRecord update = new FileRecord();
            update.setId(id);
            update.setIsLinked(1);
            update.setUpdateTime(LocalDateTime.now());
            fileRecordMapper.updateById(update);
            log.info("[文件管理] 已标记为已关联: id={}, storedPath={}", id, record.getStoredPath());
        }
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 通过真实 MIME 类型和扩展名进行安全校验。
     * <p>
     * 优先使用魔数检测出的真实 MIME 类型；同时检查扩展名作为辅助防线。
     * 白名单取配置值与默认值的并集。
     */
    private void validateFileType(String detectedMimeType, String originalFilename) {
        Set<String> allowedMimeTypes = mergeWithDefaults(
                storageProperties.getAllowedMimeTypes(), DEFAULT_ALLOWED_MIME_TYPES);
        Set<String> allowedExtensions = mergeWithDefaults(
                storageProperties.getAllowedExtensions(), DEFAULT_ALLOWED_EXTENSIONS);

        String extension = extractExtension(originalFilename).toLowerCase();

        boolean mimeAllowed = allowedMimeTypes.contains(detectedMimeType);
        boolean extAllowed = allowedExtensions.contains(extension);

        if (!mimeAllowed && !extAllowed) {
            throw BusinessException.of(400,
                    String.format("不支持的文件类型：MIME=%s, 扩展名=%s。允许的类型：%s",
                            detectedMimeType, extension, allowedExtensions));
        }

        if (!mimeAllowed) {
            log.warn("[安全校验] MIME类型不在白名单但扩展名通过：MIME={}, 扩展名={}, 文件名={}",
                    detectedMimeType, extension, originalFilename);
        }
    }

    /**
     * 提取文件扩展名（含点号，小写）。
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 将配置白名单与默认白名单合并。
     */
    private Set<String> mergeWithDefaults(String[] configured, Set<String> defaults) {
        if (configured == null || configured.length == 0) {
            return defaults;
        }
        Set<String> merged = new HashSet<>(defaults);
        merged.addAll(Arrays.asList(configured));
        return merged;
    }

    /**
     * 函数式接口：将文件内容写入 OutputStream。
     * 用于统一 MultipartFile 和 InputStream 两种上传方式的处理流程。
     */
    @FunctionalInterface
    private interface StreamWriter {
        void write(OutputStream outputStream) throws IOException;
    }

    /**
     * 自定义未检查 IO 异常（内部使用，用于在 Lambda 中包装 IOException）。
     */
    private static class UncheckedIOException extends RuntimeException {
        UncheckedIOException(IOException cause) {
            super(cause);
        }
    }
}
