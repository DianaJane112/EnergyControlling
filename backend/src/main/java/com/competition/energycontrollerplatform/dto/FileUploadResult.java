package com.competition.energycontrollerplatform.dto;

import com.competition.energycontrollerplatform.entity.FileRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传响应 DTO。
 * <p>
 * 返回给前端的最小必要信息：文件 ID、原始名、存储名、大小、访问 URL、存储类型。
 */
@Data
@Schema(description = "文件上传结果")
public class FileUploadResult {

    @Schema(description = "文件记录ID", example = "1")
    private Long id;

    @Schema(description = "原始文件名", example = "电表123-安装照片.jpg")
    private String originalName;

    @Schema(description = "存储文件名", example = "a1b2c3d4e5f6.jpg")
    private String storedName;

    @Schema(description = "文件大小（字节）", example = "204800")
    private Long fileSize;

    @Schema(description = "外部访问URL（云存储有值，本地为null由前端拼接）")
    private String accessUrl;

    @Schema(description = "存储类型", example = "LOCAL")
    private String storageType;

    @Schema(description = "真实MIME类型", example = "image/jpeg")
    private String mimeType;

    /**
     * 由 FileRecord 实体构建。
     */
    public static FileUploadResult from(FileRecord record) {
        FileUploadResult result = new FileUploadResult();
        result.setId(record.getId());
        result.setOriginalName(record.getOriginalName());
        result.setStoredName(record.getStoredName());
        result.setFileSize(record.getFileSize());
        result.setAccessUrl(record.getAccessUrl());
        result.setStorageType(record.getStorageType());
        result.setMimeType(record.getMimeType());
        return result;
    }
}
