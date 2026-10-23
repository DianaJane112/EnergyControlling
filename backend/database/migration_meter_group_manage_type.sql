-- Add management type for meter groups (BILLING or METERING).
-- Existing rows are intentionally classified as METERING until business confirms otherwise.
SET @manage_type_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 't_meter_group'
       AND COLUMN_NAME = 'manage_type'
);
SET @manage_type_ddl = IF(
    @manage_type_exists = 0,
    'ALTER TABLE t_meter_group ADD COLUMN manage_type VARCHAR(16) NOT NULL DEFAULT ''METERING'' COMMENT ''分组管理类型：BILLING计费管理，METERING计量管理'' AFTER group_name',
    'SELECT ''manage_type already exists'' AS migration_info'
);
PREPARE manage_type_stmt FROM @manage_type_ddl;
EXECUTE manage_type_stmt;
DEALLOCATE PREPARE manage_type_stmt;

UPDATE t_meter_group
   SET manage_type = 'METERING'
 WHERE manage_type IS NULL OR manage_type = '';

ALTER TABLE t_meter_group
    MODIFY COLUMN manage_type VARCHAR(16) NOT NULL DEFAULT 'METERING' COMMENT '分组管理类型：BILLING计费管理，METERING计量管理';
