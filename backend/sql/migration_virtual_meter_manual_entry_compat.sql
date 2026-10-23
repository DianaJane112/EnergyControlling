-- Virtual meter manual-entry compatibility migration.
-- This script is idempotent: it creates the missing topology table and adds
-- t_meter.pro_name only when an older database does not already have it.

CREATE TABLE IF NOT EXISTS t_shadow_meter (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    project_id      BIGINT          DEFAULT NULL,
    parent_meter_id BIGINT          DEFAULT NULL,
    child_meter_id  BIGINT          NOT NULL,
    shadow_type     VARCHAR(32)     DEFAULT NULL,
    coefficient     DECIMAL(10,4)   DEFAULT 1.0000,
    relation_desc   VARCHAR(256)    DEFAULT NULL,
    effective_date  DATE            NOT NULL,
    expire_date     DATE            DEFAULT NULL,
    status          TINYINT         NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64)     DEFAULT NULL,
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_child (parent_meter_id, child_meter_id),
    KEY idx_project_id (project_id),
    KEY idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_pro_name := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 't_meter'
    AND COLUMN_NAME = 'pro_name'
);

SET @migration_sql := IF(
  @has_pro_name = 0,
  'ALTER TABLE t_meter ADD COLUMN pro_name VARCHAR(128) NULL COMMENT ''manual virtual-meter project name'' AFTER pro_id',
  'SELECT ''t_meter.pro_name already exists'' AS migration_status'
);

PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
