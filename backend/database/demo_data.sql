-- =====================================================
-- Energy Controller Platform — 演示数据
-- =====================================================
-- 版本：v1.0.0
-- 日期：2026-06-18
-- 说明：本脚本为甲方验收演示提供干净、专业的示例数据。
--        使用前请先执行 deploy_schema.sql 建表。
--        执行方式：mysql -u root -p < demo_data.sql
-- =====================================================
-- 演示场景：智慧能源示范园区
--   - 1 个项目
--   - 5 个商户（超市、美食广场、咖啡馆、洗衣中心、便利店）
--   - 3 个分组（B13栋 / B15栋 / B17栋）
--   - 6 个电表 + 2 个水表
--   - 2 台采集器
--   - 最近 30 天日冻结 + 日报表数据
--   - 6 张账单 + 充值流水
-- =====================================================

USE energy_platform;

-- ═══════════════════════════════════════════════════════════
-- 步骤 0：清理已有演示数据（幂等，可重复执行）
-- ═══════════════════════════════════════════════════════════
DELETE FROM t_invoice              WHERE create_by = 'demo';
DELETE FROM t_recharge_request     WHERE create_by = 'demo';
DELETE FROM t_miniapp_user         WHERE create_by = 'demo';
DELETE FROM t_water_meter_reading  WHERE create_by = 'demo';
DELETE FROM t_water_meter          WHERE create_by = 'demo';
DELETE FROM t_refund_log           WHERE create_by = 'demo';
DELETE FROM t_bill                 WHERE create_by = 'demo';
DELETE FROM t_bank_account         WHERE create_by = 'demo';
DELETE FROM t_account_flow         WHERE create_by = 'demo';
DELETE FROM t_recharge_order       WHERE create_by = 'demo';
DELETE FROM tb_meter_monthly_report WHERE 1=1;
DELETE FROM tb_meter_daily_report  WHERE 1=1;
DELETE FROM t_report_daily_electricity WHERE 1=1;
DELETE FROM t_daily_freeze_record  WHERE create_by = 'demo';
DELETE FROM t_meter_reading        WHERE create_by = 'demo';
DELETE FROM t_meter                WHERE create_by = 'demo';
DELETE FROM t_collector            WHERE create_by = 'demo';
DELETE FROM t_meter_group          WHERE create_by = 'demo';
DELETE FROM t_customer             WHERE create_by = 'demo';
DELETE FROM t_project              WHERE create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 一、项目
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_project (id, project_name, project_code, description, status, create_by) VALUES
(1001, '智慧能源示范园区', 'DEMO-PARK', '位于成都市高新区，涵盖商业楼宇 B13 / B15 / B17 三栋，实行预付费电费管理', 1, 'demo');


-- ═══════════════════════════════════════════════════════════
-- 二、商户
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_customer (id, customer_code, customer_name, area_name, address, phone, customer_type, use_nature, balance, status, create_by) VALUES
(2001, 'SH-001', '嘉和超市',       'B13栋', '高新区天府大道 100 号 B13 栋 1 层 101-103', '13880001001', '商业', '一般工商业', 8520.50, 1, 'demo'),
(2002, 'SH-002', '园区美食广场',   'B13栋', '高新区天府大道 100 号 B13 栋 B1 层',       '13880001002', '商业', '一般工商业', 3200.00, 1, 'demo'),
(2003, 'SH-003', '创客咖啡馆',     'B15栋', '高新区天府大道 100 号 B15 栋 1 层 106',     '13880001003', '商业', '一般工商业', 1680.00, 1, 'demo'),
(2004, 'SH-004', '便民洗衣中心',   'B17栋', '高新区天府大道 100 号 B17 栋 1 层 101',     '13880001004', '商业', '一般工商业', 5200.00, 1, 'demo'),
(2005, 'SH-005', '邻里便利店',     'B15栋', '高新区天府大道 100 号 B15 栋 1 层 102-103', '13880001005', '商业', '一般工商业',  850.00, 1, 'demo');


-- ═══════════════════════════════════════════════════════════
-- 三、分组（按楼栋划分）
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_meter_group (id, parent_id, group_name, description, merchant_id, status, create_by) VALUES
(3001, NULL, 'B13栋电表组', 'B13 栋所有电表',       2001, 1, 'demo'),
(3002, NULL, 'B15栋电表组', 'B15 栋所有电表及水表', 2002, 1, 'demo'),
(3003, NULL, 'B17栋电表组', 'B17 栋便民洗衣中心',   2004, 1, 'demo');


-- ═══════════════════════════════════════════════════════════
-- 四、采集器
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_collector (id, customer_id, collector_no, collector_name, collector_type, ip_address, port, online, status, create_by) VALUES
(4001, 2001, 'COLL-B13-01', 'B13栋集中器', '4G无线', '10.0.1.101', 502, 1, 1, 'demo'),
(4002, 2003, 'COLL-B15-01', 'B15栋集中器', '4G无线', '10.0.1.102', 502, 1, 1, 'demo');


-- ═══════════════════════════════════════════════════════════
-- 五、电表（6个）
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_meter (id, customer_id, group_id, steal_no, vender, meter_type_name,
    caliber, terminal_name, db_no, name, bei, bjlx, meter_type, pro_id, pro_name,
    status, online, plus_price, price_type, price_content, feetype, collector_device_no, create_by) VALUES
-- B13栋
(5001, 2001, 3001, 'SN-B13-001', '青岛积成电子', '三相四线', '1.6', 'B13栋集中器', 'ELEC-B13001', '嘉和超市-电表1',  1, 1, 1, 1001, '智慧能源示范园区', 1, 1, 0.8500, 1, '{"price":0.85}', 1, 'COLL-B13-01', 'demo'),
(5002, 2002, 3001, 'SN-B13-002', '江苏远传',     '三相四线', '2.0', 'B13栋集中器', 'ELEC-B13002', '园区美食广场-电表1', 1, 1, 1, 1001, '智慧能源示范园区', 1, 1, 0.8500, 1, '{"price":0.85}', 1, 'COLL-B13-01', 'demo'),
(5003, 2001, 3001, 'SN-B13-003', '青岛积成电子', '总表',     '2.0', 'B13栋集中器', 'ELEC-B13003', '嘉和超市-总表',    1, 2, 1, 1001, '智慧能源示范园区', 1, 1, 0.8500, 1, '{"price":0.85}', 1, 'COLL-B13-01', 'demo'),
-- B15栋
(5004, 2003, 3002, 'SN-B15-001', '宁波水表',     '三相四线', '1.6', 'B15栋集中器', 'ELEC-B15001', '创客咖啡馆-电表1',  1, 1, 1, 1001, '智慧能源示范园区', 1, 1, 0.8500, 1, '{"price":0.85}', 1, 'COLL-B15-01', 'demo'),
(5005, 2005, 3002, 'SN-B15-002', '江苏远传',     '三相四线', '1.6', 'B15栋集中器', 'ELEC-B15002', '邻里便利店-电表1',  1, 1, 1, 1001, '智慧能源示范园区', 1, 1, 0.8500, 1, '{"price":0.85}', 1, 'COLL-B15-01', 'demo'),
-- B17栋
(5006, 2004, 3003, 'SN-B17-001', '青岛积成电子', '三相四线', '2.0', 'B15栋集中器', 'ELEC-B17001', '便民洗衣中心-电表1', 1, 1, 1, 1001, '智慧能源示范园区', 1, 1, 0.8800, 1, '{"price":0.88}', 1, 'COLL-B15-01', 'demo');


-- ═══════════════════════════════════════════════════════════
-- 六、水表（2个）
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_water_meter (meter_id, merchant_id, device_no, meter_name, caliber, status, online, install_time, create_by) VALUES
(6001, 2001, 'WATER-B13001', '嘉和超市-水表',   'DN20', 1, 1, '2026-01-15 10:00:00', 'demo'),
(6002, 2002, 'WATER-B13002', '园区美食广场-水表', 'DN25', 1, 1, '2026-01-15 10:00:00', 'demo');


-- ═══════════════════════════════════════════════════════════
-- 七、日冻结记录（最近 30 天，2026-05-19 ~ 2026-06-17）
--    用电量模拟：工作日高、周末低，不同商户规模不同
--    daily_usage = end_reading - start_reading
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_daily_freeze_record (meter_id, freeze_date, start_reading, end_reading,
    daily_usage, peak_usage, busy_usage, normal_usage, idle_usage, create_by)
SELECT
    m.id,
    cal.freeze_date,
    -- start_reading: 累积读数（初始值 + 每天递增）
    (base.base_reading + DATEDIFF(cal.freeze_date, '2026-05-18') * base.daily_avg) AS start_reading,
    (base.base_reading + DATEDIFF(cal.freeze_date, '2026-05-18') * base.daily_avg + base.daily_avg) AS end_reading,
    -- daily_usage: 基础日用量 × 周末系数
    ROUND(base.daily_avg * CASE WHEN DAYOFWEEK(cal.freeze_date) IN (1,7) THEN 0.65 ELSE 1.0 END, 4),
    -- peak_usage: 总用量 10-15%
    ROUND(base.daily_avg * CASE WHEN DAYOFWEEK(cal.freeze_date) IN (1,7) THEN 0.65 ELSE 1.0 END * 0.12, 4),
    -- busy_usage: 总用量 35-40%
    ROUND(base.daily_avg * CASE WHEN DAYOFWEEK(cal.freeze_date) IN (1,7) THEN 0.65 ELSE 1.0 END * 0.38, 4),
    -- normal_usage: 总用量 30-35%
    ROUND(base.daily_avg * CASE WHEN DAYOFWEEK(cal.freeze_date) IN (1,7) THEN 0.65 ELSE 1.0 END * 0.32, 4),
    -- idle_usage: 总用量 15-20%
    ROUND(base.daily_avg * CASE WHEN DAYOFWEEK(cal.freeze_date) IN (1,7) THEN 0.65 ELSE 1.0 END * 0.18, 4),
    'demo'
FROM (
    SELECT 5001 AS id, 120.0 AS daily_avg, 50000.0 AS base_reading
    UNION ALL SELECT 5002,  85.0, 38000.0
    UNION ALL SELECT 5003, 145.0, 62000.0
    UNION ALL SELECT 5004,  45.0, 18000.0
    UNION ALL SELECT 5005,  30.0, 12000.0
    UNION ALL SELECT 5006,  95.0, 42000.0
) base
JOIN t_meter m ON m.id = base.id
CROSS JOIN (
    SELECT '2026-05-19' AS freeze_date UNION ALL SELECT '2026-05-20' UNION ALL SELECT '2026-05-21'
    UNION ALL SELECT '2026-05-22' UNION ALL SELECT '2026-05-23' UNION ALL SELECT '2026-05-24'
    UNION ALL SELECT '2026-05-25' UNION ALL SELECT '2026-05-26' UNION ALL SELECT '2026-05-27'
    UNION ALL SELECT '2026-05-28' UNION ALL SELECT '2026-05-29' UNION ALL SELECT '2026-05-30'
    UNION ALL SELECT '2026-05-31' UNION ALL SELECT '2026-06-01' UNION ALL SELECT '2026-06-02'
    UNION ALL SELECT '2026-06-03' UNION ALL SELECT '2026-06-04' UNION ALL SELECT '2026-06-05'
    UNION ALL SELECT '2026-06-06' UNION ALL SELECT '2026-06-07' UNION ALL SELECT '2026-06-08'
    UNION ALL SELECT '2026-06-09' UNION ALL SELECT '2026-06-10' UNION ALL SELECT '2026-06-11'
    UNION ALL SELECT '2026-06-12' UNION ALL SELECT '2026-06-13' UNION ALL SELECT '2026-06-14'
    UNION ALL SELECT '2026-06-15' UNION ALL SELECT '2026-06-16' UNION ALL SELECT '2026-06-17'
) cal;


-- ═══════════════════════════════════════════════════════════
-- 八、用电量日报表 t_report_daily_electricity
--    从日冻结记录中提取，增加正向/反向有功、分时扣费
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_report_daily_electricity (report_date, meter_id, customer_id, group_id,
    electricity_consumed, forward_consumed, reverse_consumed,
    peak_consumed, busy_consumed, normal_consumed, idle_consumed,
    peak_charge_amount, busy_charge_amount, normal_charge_amount, idle_charge_amount,
    charge_amount)
SELECT
    f.freeze_date,
    f.meter_id,
    m.customer_id,
    m.group_id,
    f.daily_usage,
    f.daily_usage,
    0.0000,
    f.peak_usage,
    f.busy_usage,
    f.normal_usage,
    f.idle_usage,
    ROUND(f.peak_usage  * COALESCE(m.plus_price, 0.85), 4),
    ROUND(f.busy_usage  * COALESCE(m.plus_price, 0.85), 4),
    ROUND(f.normal_usage * COALESCE(m.plus_price, 0.85), 4),
    ROUND(f.idle_usage  * COALESCE(m.plus_price, 0.85), 4),
    ROUND(f.daily_usage * COALESCE(m.plus_price, 0.85), 4)
FROM t_daily_freeze_record f
JOIN t_meter m ON m.id = f.meter_id
WHERE f.create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 九、表计日报表 tb_meter_daily_report
--    水+电统一，供 MeterReportController 查询
-- ═══════════════════════════════════════════════════════════
INSERT INTO tb_meter_daily_report (project_id, group_id, merchant_id, device_no,
    meter_id, meter_type, total_usage, peak_usage, busy_usage, normal_usage, idle_usage, stat_date)
SELECT
    m.pro_id,
    m.group_id,
    m.customer_id,
    COALESCE(m.db_no, m.steal_no),
    m.id,
    'ELECTRIC',
    rde.electricity_consumed,
    rde.peak_consumed,
    rde.busy_consumed,
    rde.normal_consumed,
    rde.idle_consumed,
    rde.report_date
FROM t_report_daily_electricity rde
JOIN t_meter m ON m.id = rde.meter_id
WHERE m.create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 十、表计月报表 tb_meter_monthly_report（2026年5月）
-- ═══════════════════════════════════════════════════════════
INSERT INTO tb_meter_monthly_report (project_id, group_id, merchant_id, device_no,
    meter_id, meter_type, total_usage, peak_usage, busy_usage, normal_usage, idle_usage,
    stat_month, stat_year, daily_count, create_time)
SELECT
    m.pro_id,
    m.group_id,
    m.customer_id,
    COALESCE(m.db_no, m.steal_no),
    m.id,
    'ELECTRIC',
    ROUND(SUM(rde.electricity_consumed), 4),
    ROUND(SUM(rde.peak_consumed), 4),
    ROUND(SUM(rde.busy_consumed), 4),
    ROUND(SUM(rde.normal_consumed), 4),
    ROUND(SUM(rde.idle_consumed), 4),
    '2026-05',
    '2026',
    COUNT(DISTINCT rde.report_date),
    NOW()
FROM t_report_daily_electricity rde
JOIN t_meter m ON m.id = rde.meter_id
WHERE rde.report_date BETWEEN '2026-05-01' AND '2026-05-31'
  AND m.create_by = 'demo'
GROUP BY m.id;


-- ═══════════════════════════════════════════════════════════
-- 十一、水表读数流水（最近 30 天，每 3 天一条）
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_water_meter_reading (meter_id, record_time, usage_amount, fee, create_by)
SELECT
    wm.meter_id,
    cal.record_time,
    ROUND(2.5 + RAND() * 5.0, 4) AS usage_amount,
    ROUND((2.5 + RAND() * 5.0) * 3.20, 4) AS fee,
    'demo'
FROM t_water_meter wm
CROSS JOIN (
    SELECT '2026-05-19 08:00:00' AS record_time UNION ALL SELECT '2026-05-22 08:00:00'
    UNION ALL SELECT '2026-05-25 08:00:00' UNION ALL SELECT '2026-05-28 08:00:00'
    UNION ALL SELECT '2026-05-31 08:00:00' UNION ALL SELECT '2026-06-03 08:00:00'
    UNION ALL SELECT '2026-06-06 08:00:00' UNION ALL SELECT '2026-06-09 08:00:00'
    UNION ALL SELECT '2026-06-12 08:00:00' UNION ALL SELECT '2026-06-15 08:00:00'
) cal
WHERE wm.create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 十二、账单分类
-- ═══════════════════════════════════════════════════════════
INSERT IGNORE INTO t_bill_category (id, category_name, category_code, status) VALUES
(1, '电费',   'ELECTRIC', 1),
(2, '水费',   'WATER',    1),
(3, '物业费', 'PROPERTY', 1);


-- ═══════════════════════════════════════════════════════════
-- 十三、账单（6张，覆盖已支付/未支付/待审核）
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_bill (bill_no, bill_name, category_id, category_name, meter_id,
    meter_no, merchant_name, received_amount, pending_amount, payment_status,
    arrears_handling, support_online_payment, description,
    bill_create_time, last_payment_time, actual_payment_time, remark, create_by) VALUES
('DEMO-202605001', '2026年5月电费',   1, '电费',   5001, 'ELEC-B13001', '嘉和超市',     3850.00,    0, '已支付', '余额扣除', 1,
    'B13栋嘉和超市5月电费（用量约4529kWh）',     '2026-05-31 08:00:00', '2026-06-01 09:30:00', '2026-06-01 09:30:00', '5月电费已结清', 'demo'),
('DEMO-202605002', '2026年5月电费',   1, '电费',   5002, 'ELEC-B13002', '园区美食广场', 2760.00,    0, '已支付', '余额扣除', 1,
    'B13栋园区美食广场5月电费（用量约3247kWh）', '2026-05-31 08:05:00', '2026-06-02 14:20:00', '2026-06-02 14:20:00', '', 'demo'),
('DEMO-202605003', '2026年5月电费',   1, '电费',   5004, 'ELEC-B15001', '创客咖啡馆',   1250.00,    0, '已支付', '余额扣除', 1,
    'B15栋创客咖啡馆5月电费（用量约1470kWh）',   '2026-05-31 08:10:00', '2026-06-01 10:15:00', '2026-06-01 10:15:00', '', 'demo'),
('DEMO-202606001', '2026年6月电费',   1, '电费',   5005, 'ELEC-B15002', '邻里便利店',       0,  680.00, '未支付', '跳闸',    1,
    'B15栋邻里便利店6月电费（截至6月17日）',     '2026-06-17 08:00:00', NULL, NULL, '商户余额不足，待缴费', 'demo'),
('DEMO-202605004', '2026年5月水费',   2, '水费',   NULL, 'WATER-B13001','嘉和超市',      320.00,    0, '已支付', '余额扣除', 0,
    'B13栋嘉和超市5月水费',                      '2026-05-31 08:15:00', '2026-06-01 09:00:00', '2026-06-01 09:00:00', '', 'demo'),
('DEMO-202606002', '2026年Q2物业费',  3, '物业费', NULL, NULL,          '嘉和超市',     2800.00,    0, '待审核', '限制交费', 1,
    'B13栋嘉和超市4-6月物业管理费',              '2026-06-01 08:30:00', NULL, NULL, '待财务审核入账', 'demo');


-- ═══════════════════════════════════════════════════════════
-- 十四、充值订单 + 资金流水
-- ═══════════════════════════════════════════════════════════
-- 充值订单
INSERT INTO t_recharge_order (order_no, customer_id, meter_id, amount, balance_before, balance_after, order_status, create_time, create_by) VALUES
('RECHG-20260515001', 2001, 5001, 5000.00, 3520.50,  8520.50,  'SUCCESS', '2026-05-15 10:00:00', 'demo'),
('RECHG-20260520001', 2002, 5002, 3000.00,  200.00,  3200.00,  'SUCCESS', '2026-05-20 14:30:00', 'demo'),
('RECHG-20260601001', 2004, 5006, 2000.00, 3200.00,  5200.00,  'SUCCESS', '2026-06-01 09:00:00', 'demo'),
('RECHG-20260610001', 2005, 5005,  500.00,  350.00,   850.00,  'SUCCESS', '2026-06-10 16:20:00', 'demo');

-- 资金流水
INSERT INTO t_account_flow (flow_no, customer_id, meter_id, balance_before, change_amount,
    balance_after, trade_type, biz_no, operator_name, operate_time, remark, create_by) VALUES
(UUID(), 2001, 5001, 3520.50,  5000.00, 8520.50,  1, 'RECHG-20260515001', '系统管理员', '2026-05-15 10:00:00', '嘉和超市预存电费',      'demo'),
(UUID(), 2002, 5002,  200.00,  3000.00, 3200.00,  1, 'RECHG-20260520001', '系统管理员', '2026-05-20 14:30:00', '园区美食广场预存电费',  'demo'),
(UUID(), 2004, 5006, 3200.00,  2000.00, 5200.00,  1, 'RECHG-20260601001', '系统管理员', '2026-06-01 09:00:00', '便民洗衣中心预存电费',  'demo'),
(UUID(), 2005, 5005,  350.00,   500.00,  850.00,  1, 'RECHG-20260610001', '系统管理员', '2026-06-10 16:20:00', '邻里便利店预存电费',    'demo'),
-- 消费扣费流水（5月）
(UUID(), 2001, 5001, 8520.50, -3850.00, 4670.50,  2, 'DEMO-202605001', '系统自动', '2026-06-01 09:30:00', '嘉和超市5月电费扣款',   'demo'),
(UUID(), 2002, 5002, 3200.00, -2760.00,  440.00,  2, 'DEMO-202605002', '系统自动', '2026-06-02 14:20:00', '园区美食广场5月电费扣款', 'demo'),
(UUID(), 2003, 5004, 2930.00, -1250.00, 1680.00,  2, 'DEMO-202605003', '系统自动', '2026-06-01 10:15:00', '创客咖啡馆5月电费扣款',  'demo'),
(UUID(), 2001, 6001, 4670.50,  -320.00, 4350.50,  2, 'DEMO-202605004', '系统自动', '2026-06-01 09:00:00', '嘉和超市5月水费扣款',   'demo');

-- 更新商户余额（扣费后）
UPDATE t_customer SET balance = 4350.50 WHERE id = 2001 AND create_by = 'demo';
UPDATE t_customer SET balance =  440.00 WHERE id = 2002 AND create_by = 'demo';


-- ═══════════════════════════════════════════════════════════
-- 十五、银行账户
-- ═══════════════════════════════════════════════════════════
INSERT INTO t_bank_account (id, customer_id, bank_name, account_no, account_name, status, create_by) VALUES
(7001, 2001, '中国工商银行成都高新支行', '6222024402012345678', '嘉和超市',           1, 'demo'),
(7002, 2004, '中国建设银行成都天府支行', '6227003812345678901', '便民洗衣中心',       1, 'demo');


-- ═══════════════════════════════════════════════════════════
-- 验证查询
-- ═══════════════════════════════════════════════════════════
SELECT '========================================' AS '';
SELECT '  演示数据导入完成' AS '';
SELECT '========================================' AS '';
SELECT CONCAT('项目: ', COUNT(*), ' 个')   FROM t_project  WHERE create_by = 'demo';
SELECT CONCAT('商户: ', COUNT(*), ' 个')   FROM t_customer WHERE create_by = 'demo';
SELECT CONCAT('分组: ', COUNT(*), ' 个')   FROM t_meter_group WHERE create_by = 'demo';
SELECT CONCAT('采集器: ', COUNT(*), ' 台') FROM t_collector WHERE create_by = 'demo';
SELECT CONCAT('电表: ', COUNT(*), ' 个')   FROM t_meter    WHERE create_by = 'demo';
SELECT CONCAT('水表: ', COUNT(*), ' 个')   FROM t_water_meter WHERE create_by = 'demo';
SELECT CONCAT('日冻结: ', COUNT(*), ' 条') FROM t_daily_freeze_record WHERE create_by = 'demo';
SELECT CONCAT('日报表: ', COUNT(*), ' 条') FROM tb_meter_daily_report;
SELECT CONCAT('账单: ', COUNT(*), ' 张')   FROM t_bill     WHERE create_by = 'demo';
SELECT CONCAT('充值: ', COUNT(*), ' 笔')   FROM t_recharge_order WHERE create_by = 'demo';
SELECT CONCAT('流水: ', COUNT(*), ' 条')   FROM t_account_flow WHERE create_by = 'demo';
