-- =====================================================
-- 扩容迁移：t_report_daily_electricity 增加正向/反向有功字段
-- 执行方式：mysql -u root -p energy_platform < 本文件
-- =====================================================
USE energy_platform;

ALTER TABLE t_report_daily_electricity
    ADD COLUMN forward_consumed DECIMAL(18,4) DEFAULT 0.0000 COMMENT '正向有功日用电量（kWh）' AFTER electricity_consumed,
    ADD COLUMN reverse_consumed DECIMAL(18,4) DEFAULT 0.0000 COMMENT '反向有功日用电量（kWh）' AFTER forward_consumed;
