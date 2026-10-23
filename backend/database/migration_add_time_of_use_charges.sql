-- =====================================================
-- 扩容迁移：t_report_daily_electricity 增加分时电费字段
-- 尖/峰/平/谷四个时段的扣费金额，支撑复费率计费电量日报表明细展示
-- 执行方式：mysql -u root -p energy_platform < 本文件
-- =====================================================
USE energy_platform;

ALTER TABLE t_report_daily_electricity
    ADD COLUMN peak_charge_amount   DECIMAL(14,4) DEFAULT 0.0000 COMMENT '尖时段扣费金额（元）' AFTER idle_consumed,
    ADD COLUMN busy_charge_amount   DECIMAL(14,4) DEFAULT 0.0000 COMMENT '峰时段扣费金额（元）' AFTER peak_charge_amount,
    ADD COLUMN normal_charge_amount DECIMAL(14,4) DEFAULT 0.0000 COMMENT '平时段扣费金额（元）' AFTER busy_charge_amount,
    ADD COLUMN idle_charge_amount   DECIMAL(14,4) DEFAULT 0.0000 COMMENT '谷时段扣费金额（元）' AFTER normal_charge_amount;
