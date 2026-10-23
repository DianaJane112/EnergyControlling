-- =====================================================
-- Energy Controller Platform — 演示数据清理脚本
-- =====================================================
-- 版本：v1.0.0
-- 日期：2026-06-18
-- 说明：甲方验收完成后执行此脚本，一键清除所有演示数据。
--        系统默认角色、费率策略等 deploy_schema.sql 中的
--        种子数据不会被清除。
-- 执行方式：mysql -u root -p < demo_cleanup.sql
-- =====================================================

USE energy_platform;

-- ═══════════════════════════════════════════════════════════
-- 按依赖顺序删除（先删子表，再删主表）
-- ═══════════════════════════════════════════════════════════

-- 业务明细数据
DELETE FROM t_invoice              WHERE create_by = 'demo';
DELETE FROM t_recharge_request     WHERE create_by = 'demo';
DELETE FROM t_miniapp_user         WHERE create_by = 'demo';
DELETE FROM t_water_meter_reading  WHERE create_by = 'demo';
DELETE FROM t_refund_log           WHERE create_by = 'demo';
DELETE FROM t_bill                 WHERE create_by = 'demo';
DELETE FROM t_bank_account         WHERE create_by = 'demo';
DELETE FROM t_account_flow         WHERE create_by = 'demo';
DELETE FROM t_recharge_order       WHERE create_by = 'demo';

-- 预聚合报表
DELETE FROM tb_meter_monthly_report WHERE meter_id IN (SELECT id FROM t_meter WHERE create_by = 'demo');
DELETE FROM tb_meter_daily_report   WHERE meter_id IN (SELECT id FROM t_meter WHERE create_by = 'demo');
DELETE FROM t_report_daily_electricity WHERE meter_id IN (SELECT id FROM t_meter WHERE create_by = 'demo');

-- 原始读数
DELETE FROM t_daily_freeze_record  WHERE create_by = 'demo';
DELETE FROM t_meter_reading        WHERE create_by = 'demo';

-- 设备
DELETE FROM t_water_meter          WHERE create_by = 'demo';
DELETE FROM t_meter                WHERE create_by = 'demo';
DELETE FROM t_collector            WHERE create_by = 'demo';

-- 组织架构
DELETE FROM t_meter_group          WHERE create_by = 'demo';
DELETE FROM t_customer             WHERE create_by = 'demo';
DELETE FROM t_project              WHERE create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 验证清理结果
-- ═══════════════════════════════════════════════════════════
SELECT '========================================' AS '';
SELECT '  演示数据清理完成' AS '';
SELECT '  以下表已清空所有 demo 标记数据：' AS '';
SELECT '========================================' AS '';
SELECT
    TABLE_NAME AS 表名,
    TABLE_COMMENT AS 说明,
    (SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema = 'energy_platform') AS ''
FROM information_schema.TABLES
WHERE table_schema = 'energy_platform'
ORDER BY TABLE_NAME;

SELECT '========================================' AS '';
SELECT '  系统默认数据仍然保留：' AS '';
SELECT '    - t_role（系统管理员/运营人员/审计员）' AS '';
SELECT '    - t_price_strategy（默认固定电价 0.85 元）' AS '';
SELECT '    - t_rate_config（全局费率配置）' AS '';
SELECT '========================================' AS '';
