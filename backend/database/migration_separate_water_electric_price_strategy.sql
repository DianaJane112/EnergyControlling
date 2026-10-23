-- Separate water and electric price strategies without changing existing prices.
USE energy_platform;

ALTER TABLE t_price_strategy
    ADD COLUMN meter_type TINYINT NOT NULL DEFAULT 1 COMMENT '表计类型：0-水表，1-电表' AFTER project_id,
    ADD KEY idx_meter_type (meter_type);

-- A strategy referenced by both categories is copied for water meters. Electric
-- meters retain the original row so existing electric behavior is unchanged.
CREATE TEMPORARY TABLE tmp_shared_price_strategy (old_id BIGINT PRIMARY KEY);
INSERT INTO tmp_shared_price_strategy (old_id)
SELECT DISTINCT s.id
FROM t_price_strategy s
JOIN t_meter w ON w.rate_config_id = s.id AND w.meter_type = 0 AND w.is_deleted = 0
JOIN t_meter e ON e.rate_config_id = s.id AND e.meter_type = 1 AND e.is_deleted = 0
WHERE s.is_deleted = 0;

INSERT INTO t_price_strategy
    (strategy_name, strategy_code, project_id, meter_type, price_type, time_slots,
     fixed_price, plus_price, effective_date, expire_date, status, remark,
     create_time, update_time, create_by, is_deleted)
SELECT CONCAT(s.strategy_name, '-水表'),
       CONCAT(LEFT(s.strategy_code, 48), '_WATER_', s.id),
       s.project_id, 0, s.price_type, s.time_slots, s.fixed_price, s.plus_price,
       s.effective_date, s.expire_date, s.status, s.remark,
       NOW(), NOW(), s.create_by, s.is_deleted
FROM t_price_strategy s
JOIN tmp_shared_price_strategy x ON x.old_id = s.id;

UPDATE t_meter w
JOIN tmp_shared_price_strategy x ON x.old_id = w.rate_config_id
JOIN t_price_strategy copy
  ON copy.strategy_code = CONCAT(LEFT((SELECT strategy_code FROM t_price_strategy original WHERE original.id = x.old_id), 48), '_WATER_', x.old_id)
SET w.rate_config_id = copy.id
WHERE w.meter_type = 0 AND w.is_deleted = 0;

-- Strategies used only by water meters can be reclassified in place.
UPDATE t_price_strategy s
SET s.meter_type = 0
WHERE EXISTS (SELECT 1 FROM t_meter w WHERE w.rate_config_id = s.id AND w.meter_type = 0 AND w.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM t_meter e WHERE e.rate_config_id = s.id AND e.meter_type = 1 AND e.is_deleted = 0);

DROP TEMPORARY TABLE tmp_shared_price_strategy;
