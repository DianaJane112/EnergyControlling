-- =====================================================
-- 自动化计费 V2 数据库迁移脚本
-- 1. t_meter 增加结算追踪列
-- 2. 插入默认费率配置（供后续扩展）
-- =====================================================

USE energy_platform;

-- 1. 表计表增加结算追踪字段
ALTER TABLE t_meter
    ADD COLUMN last_settled_reading DECIMAL(18,4) DEFAULT NULL COMMENT '上次结算读数' AFTER online,
    ADD COLUMN last_settled_time     DATETIME      DEFAULT NULL COMMENT '上次结算时间' AFTER last_settled_reading;

-- 2. 插入固定费率示例（单价 0.85 元/kWh，无附加费）
INSERT INTO t_rate_config (meter_id, price_type, price_content, plus_price, effective_date, status, create_time, update_time)
VALUES (NULL, 1, '{"price":0.85}', 0.0000, '2026-01-01', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id=id;

-- 3. 确认迁移结果
SELECT 'Migration V2 completed' AS status;
