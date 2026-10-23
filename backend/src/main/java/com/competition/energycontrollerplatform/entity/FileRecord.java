package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录实体（映射 t_file_record 表）。
 * <p>
 * 每上传一个文件即生成一条记录，包含原始文件名、存储路径、真实 MIME 类型等信息。
 * {@code isLinked} 字段用于标记该文件是否已关联到具体业务，
 * 配合定时任务清理长期未关联的孤儿文件。
 */
@Data
@TableName("t_file_record")
public class FileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名（含扩展名，如 "电表123-安装照片.jpg"） */
    private String originalName;

    /** 存储后的唯一文件名（UUID + 扩展名，如 "a1b2c3d4.jpg"） */
    private String storedName;

    /** 存储路径/对象Key（含日期前缀，如 "2026/06/17/a1b2c3d4.jpg"） */
    private String storedPath;

    /** 外部访问 URL（云存储有值，本地存储为 null 由服务端动态拼接） */
    private String accessUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 真实 MIME 类型（通过魔数检测得出，非用户声称值） */
    private String mimeType;

    /** 文件扩展名（小写，含点号，如 ".jpg"） */
    private String extension;

    /** 存储类型编码（LOCAL / OSS / MINIO） */
    private String storageType;

    /** 上传人用户 ID */
    private Long uploaderId;

    /** 上传人用户名 */
    private String uploaderName;

    /** 业务模块（report / QR / bill / contract / meter-photo） */
    private String module;

    /** 关联的业务记录 ID（如 reportId, meterId） */
    private Long refId;

    /**
     * 是否已关联到业务。
     * <ul>
     *   <li>0 — 刚上传，尚未与具体业务数据绑定</li>
     *   <li>1 — 已绑定业务数据，受保护不会被孤儿清理任务删除</li>
     * </ul>
     */
    private Integer isLinked;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    // ────────────── 便捷方法 ──────────────

    public boolean isLinked() {
        return this.isLinked != null && this.isLinked == 1;
    }

    public boolean isLocal() {
        return "LOCAL".equalsIgnoreCase(this.storageType);
    }
}
