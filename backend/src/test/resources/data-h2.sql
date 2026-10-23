-- =====================================================
-- Energy Controller Platform — H2 Test Seed Data
-- =====================================================

-- 测试用户（密码为 BCrypt 编码的 "Test@1234!"）
INSERT INTO t_user (username, password, real_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 'ADMIN', 1),
('operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '运营人员', 'OPERATOR', 1);

-- 测试商户
INSERT INTO t_customer (customer_code, customer_name, balance, status) VALUES
('C001', '测试商户一号', 1000.0000, 1),
('C002', '测试商户二号', 500.0000, 1),
('C003', '零余额商户', 0.0000, 1);

-- 测试电表
INSERT INTO t_meter (db_no, name, customer_id, meter_type, vender, status, online, state_valve, bjlx, feetype, credit, warnline, bei) VALUES
('DB0000000001', '测试电表1', 1, 1, 'chenman', 1, 1, 1, 1, 1, 100.0000, 50.0000, 1),
('DB0000000002', '测试电表2', 1, 1, 'chenman', 1, 1, 1, 2, 2, 0.0000, 0.0000, 1),
('DB0000000003', '测试电表3', 2, 1, 'haopu', 0, 0, 2, 3, 1, 200.0000, 100.0000, 5);

-- 测试水表
INSERT INTO t_meter (steal_no, db_no, name, customer_id, meter_type, vender, status, online, state_valve, bjlx, feetype) VALUES
('ST000001', 'WM0000000001', '测试水表1', 1, 0, 'jicheng', 1, 1, 1, 4, 1),
('ST000002', 'WM0000000002', '测试水表2', 2, 0, 'jiexian', 1, 0, 2, 4, 2);
