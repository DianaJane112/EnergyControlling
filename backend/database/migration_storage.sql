-- =====================================================
-- Migration: 文件存储系统 (t_file_record)
-- =====================================================
-- 功能：统一文件管理——支持本地磁盘、阿里云OSS、MinIO及其他S3兼容对象存储
-- 日期前缀路径（yyyy/MM/dd/）避免单目录文件数过大导致性能下降
-- =====================================================

CREATE TABLE IF NOT EXISTS t_file_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    original_name   VARCHAR(512)    NOT NULL                 COMMENT '原始文件名',
    stored_name     VARCHAR(128)    NOT NULL                 COMMENT '存储文件名（UUID）',
    stored_path     VARCHAR(1024)   NOT NULL                 COMMENT '存储路径/对象Key（含日期前缀，如 2026/06/17/uuid.jpg）',
    access_url      VARCHAR(2048)   DEFAULT NULL             COMMENT '外部访问URL（云存储有值，本地为NULL由服务端拼接）',
    file_size       BIGINT          NOT NULL DEFAULT 0       COMMENT '文件大小（字节）',
    mime_type       VARCHAR(128)    DEFAULT NULL             COMMENT '真实MIME类型（通过魔数检测，非用户声称值）',
    extension       VARCHAR(32)     DEFAULT NULL             COMMENT '文件扩展名（小写）',
    storage_type    VARCHAR(32)     NOT NULL                 COMMENT '存储类型：LOCAL / OSS / MINIO',
    uploader_id     BIGINT          DEFAULT NULL             COMMENT '上传人ID',
    uploader_name   VARCHAR(64)     DEFAULT NULL             COMMENT '上传人用户名',
    module          VARCHAR(64)     DEFAULT NULL             COMMENT '业务模块（report/QR/bill/contract/meter-photo）',
    ref_id          BIGINT          DEFAULT NULL             COMMENT '关联业务ID',
    is_linked       TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已关联业务：0=临时/待关联，1=已关联（防止孤儿文件）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除（0=正常，1=已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stored_name (stored_name),
    KEY idx_uploader_id (uploader_id),
    KEY idx_module_ref (module, ref_id),
    KEY idx_storage_type (storage_type),
    KEY idx_create_time (create_time),
    KEY idx_is_linked (is_linked, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';
