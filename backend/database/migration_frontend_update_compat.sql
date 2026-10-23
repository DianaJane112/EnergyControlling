-- 新版前端兼容迁移：区分真实/虚拟表，并补充计费轨迹快照字段。
-- 适用于已部署过旧版本 schema 的 MySQL 数据库，可重复执行。
USE energy_platform;

CREATE TABLE IF NOT EXISTS t_meter_operation_trace (
  id BIGINT NOT NULL AUTO_INCREMENT,
  object_type VARCHAR(32) NOT NULL,
  object_id BIGINT NULL,
  object_no VARCHAR(128) NULL,
  project_id BIGINT NULL,
  customer_id BIGINT NULL,
  customer_code_snapshot VARCHAR(64) NULL,
  customer_name_snapshot VARCHAR(128) NULL,
  meter_id BIGINT NULL,
  parent_meter_id BIGINT NULL,
  child_meter_id BIGINT NULL,
  event_type VARCHAR(32) NOT NULL,
  operation_terminal VARCHAR(16) DEFAULT 'API',
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  balance_before DECIMAL(18,4) NULL,
  change_amount DECIMAL(18,4) NULL,
  credited_amount DECIMAL(18,4) NULL,
  balance_after DECIMAL(18,4) NULL,
  previous_reading DECIMAL(18,4) NULL,
  raw_measurement DECIMAL(18,4) NULL,
  billed_measurement DECIMAL(18,4) NULL,
  consumption DECIMAL(18,4) NULL,
  ct_ratio DECIMAL(10,4) NULL,
  pt_ratio DECIMAL(10,4) NULL,
  unit_price DECIMAL(10,4) NULL,
  plus_price DECIMAL(10,4) NULL,
  rate_config_id BIGINT NULL,
  price_type TINYINT NULL,
  billing_type VARCHAR(32) NULL,
  payment_method VARCHAR(32) NULL,
  relation_before TEXT NULL,
  relation_after TEXT NULL,
  remark VARCHAR(512) NULL,
  request_id VARCHAR(128) NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  result_status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  failure_reason VARCHAR(1000) NULL,
  detail_json JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_trace_object (object_type, object_id, operation_time),
  KEY idx_trace_event (event_type, operation_time),
  KEY idx_trace_customer (customer_id, operation_time),
  UNIQUE KEY uk_trace_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计及虚拟表业务操作轨迹';

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing$$
CREATE PROCEDURE add_column_if_missing(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS add_index_if_missing$$
CREATE PROCEDURE add_index_if_missing(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL add_column_if_missing('t_meter', 'meter_kind',
    'VARCHAR(16) NOT NULL DEFAULT ''PHYSICAL'' COMMENT ''表计性质：PHYSICAL-真实表，VIRTUAL-虚拟结算表'' AFTER `customer_id`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'previous_reading', 'DECIMAL(18,4) NULL AFTER `balance_after`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'ct_ratio', 'DECIMAL(10,4) NULL AFTER `consumption`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'pt_ratio', 'DECIMAL(10,4) NULL AFTER `ct_ratio`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'unit_price', 'DECIMAL(10,4) NULL AFTER `pt_ratio`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'plus_price', 'DECIMAL(10,4) NULL AFTER `unit_price`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'rate_config_id', 'BIGINT NULL AFTER `plus_price`')$$
CALL add_column_if_missing('t_meter_operation_trace', 'price_type', 'TINYINT NULL AFTER `rate_config_id`')$$
CALL add_index_if_missing('t_meter', 'idx_meter_kind_type_deleted', '`meter_kind`, `meter_type`, `is_deleted`')$$

DROP PROCEDURE IF EXISTS add_column_if_missing$$
DROP PROCEDURE IF EXISTS add_index_if_missing$$

DELIMITER ;

-- 历史数据回填；显式虚拟表来源和自动生成的 VM* 表号标记为 VIRTUAL。
UPDATE t_meter
SET meter_kind = CASE
    WHEN create_by = 'MERCHANT_VIRTUAL_METER'
      OR db_no LIKE 'VME%'
      OR db_no LIKE 'VMW%'
    THEN 'VIRTUAL'
    ELSE 'PHYSICAL'
END
WHERE meter_kind IS NULL
   OR meter_kind = ''
   OR (meter_kind = 'PHYSICAL' AND (
       create_by = 'MERCHANT_VIRTUAL_METER' OR db_no LIKE 'VME%' OR db_no LIKE 'VMW%'
   ));
