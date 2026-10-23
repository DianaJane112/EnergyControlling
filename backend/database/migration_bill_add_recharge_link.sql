-- ============================================================
-- 即时账单 ←→ 小程序充值申请 关联字段
-- 创建日期: 2026-06-15
-- 说明: t_bill 新增 recharge_request_id 字段，关联 t_recharge_request
-- 用法: mysql -u root -p energy_platform < database/migration_bill_add_recharge_link.sql
-- ============================================================
USE energy_platform;

ALTER TABLE t_bill ADD COLUMN recharge_request_id BIGINT DEFAULT NULL
    COMMENT '关联小程序充值申请ID (FK→t_recharge_request.id)' AFTER meter_id;

-- 验证
SELECT '--- 检查结果 ---' AS info;
DESC t_bill;
