-- t_meter_reading 兼容迁移
-- 用途：兼容早期现场数据库，补齐昊普入库及首页/水表趋势统计所需字段。
-- 可重复执行：仅在字段不存在时新增，不删除、不覆盖已有数据。

SET @db_name = DATABASE();

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'report'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN report TINYINT(1) DEFAULT NULL COMMENT ''昊普上报标记'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'is_deleted'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''逻辑删除'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'read_val'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN read_val DECIMAL(18,4) DEFAULT NULL COMMENT ''水表读数'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'data_value0'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN data_value0 DECIMAL(18,4) DEFAULT NULL COMMENT ''捷先数据值0'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'data_dt'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN data_dt VARCHAR(32) DEFAULT NULL COMMENT ''源数据日期'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'update_time'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND column_name = 'create_by'),
    'SELECT 1',
    'ALTER TABLE t_meter_reading ADD COLUMN create_by VARCHAR(64) DEFAULT NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @db_name AND table_name = 't_meter_reading'
              AND index_name = 'idx_meter_reading_meter_time'),
    'SELECT 1',
    'CREATE INDEX idx_meter_reading_meter_time ON t_meter_reading (meter_id, create_time)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 执行后核对字段
SHOW COLUMNS FROM t_meter_reading;
