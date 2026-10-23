-- =====================================================
-- 分组管理添加 parent_id 层级支持
-- 执行方式：mysql -uroot -proot energy_platform < migration_add_group_parent_id.sql
-- =====================================================
ALTER TABLE t_meter_group ADD COLUMN IF NOT EXISTS parent_id BIGINT DEFAULT NULL COMMENT '父分组ID，NULL表示顶级分组' AFTER id;
ALTER TABLE t_meter_group ADD INDEX IF NOT EXISTS idx_parent_id (parent_id);
