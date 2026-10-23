-- Idempotent compatibility migration. No credentials or signature fields are changed.
CREATE TABLE IF NOT EXISTS t_virtual_meter_viewer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    virtual_meter_id BIGINT NOT NULL COMMENT 't_meter.id; meter_kind must be VIRTUAL',
    miniapp_user_id BIGINT NOT NULL COMMENT 't_miniapp_user.id',
    permission_type VARCHAR(16) NOT NULL DEFAULT 'VIEW',
    status TINYINT NOT NULL DEFAULT 1,
    effective_time DATETIME DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_virtual_meter_viewer (virtual_meter_id, miniapp_user_id),
    INDEX idx_virtual_meter_viewer_user (miniapp_user_id),
    INDEX idx_virtual_meter_viewer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟表查看用户授权';

-- Historical virtual meters returned here need a manually selected account.
SELECT id, db_no, name FROM t_meter
WHERE meter_kind = 'VIRTUAL' AND customer_id IS NULL AND is_deleted = 0;
