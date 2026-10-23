-- =====================================================
-- Phase 5: 基站/集中器管理 + 电表恢复 + 报表深化
-- 迁移日期: 2026-06-01
-- =====================================================

-- 1. t_collector 增加 customer_id 列（商户隔离）
ALTER TABLE t_collector
    ADD COLUMN customer_id BIGINT DEFAULT NULL COMMENT '关联商户ID（平台侧权限隔离）' AFTER id;

-- 2. t_collector 增加 customer_id 索引
ALTER TABLE t_collector
    ADD INDEX idx_customer_id (customer_id);
