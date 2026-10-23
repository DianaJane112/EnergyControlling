-- =====================================================
-- 仪表盘 V3 数据库迁移脚本
-- 1. t_meter_reading 增加 create_time 索引（支撑7天能耗趋势 GROUP BY 查询）
-- =====================================================

USE energy_platform;

-- 1. 为 7 天能耗趋势聚合查询创建索引
--    Dashboard 查询: WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(create_time)
ALTER TABLE t_meter_reading
    ADD INDEX idx_create_time (create_time);

-- 2. 确认迁移结果
SELECT 'Migration Dashboard V3 completed' AS status;
