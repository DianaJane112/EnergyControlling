-- Versioned tariffs, dimensional groups and immutable billing segments.
-- Does not modify signatures, credentials, endpoint URLs or production configuration.
ALTER TABLE t_meter_group ADD COLUMN dimension_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL' AFTER manage_type;
ALTER TABLE t_price_strategy ADD COLUMN version_group_code VARCHAR(64) NULL AFTER strategy_code;
ALTER TABLE t_price_strategy ADD COLUMN version_no INT NOT NULL DEFAULT 1 AFTER version_group_code;
UPDATE t_price_strategy SET version_group_code=strategy_code WHERE version_group_code IS NULL;
CREATE INDEX idx_price_version_period ON t_price_strategy(version_group_code,meter_type,effective_date,expire_date,status);

CREATE TABLE IF NOT EXISTS t_billing_segment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 settlement_no VARCHAR(64) NOT NULL, virtual_meter_id BIGINT NOT NULL,
 source_meter_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, billing_date DATE NOT NULL,
 period_start DATETIME NULL, period_end DATETIME NOT NULL,
 previous_reading DECIMAL(20,6), current_reading DECIMAL(20,6),
 raw_consumption DECIMAL(20,6) NOT NULL, billed_consumption DECIMAL(20,6) NOT NULL,
 ct_ratio DECIMAL(12,6), pt_ratio DECIMAL(12,6), strategy_id BIGINT NOT NULL,
 strategy_code VARCHAR(64), strategy_version INT, price_type INT,
 unit_price DECIMAL(18,8), plus_price DECIMAL(18,8), charge_amount DECIMAL(18,4) NOT NULL,
 project_id BIGINT, project_name VARCHAR(128), area_group_id BIGINT, area_name VARCHAR(128),
 category_group_id BIGINT, category_name VARCHAR(128), detail_json JSON,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
 INDEX idx_billing_segment_period(billing_date,customer_id),
 INDEX idx_billing_segment_dimension(project_id,area_group_id,category_group_id),
 INDEX idx_billing_segment_meter(virtual_meter_id,source_meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变计费分段与维度快照';
