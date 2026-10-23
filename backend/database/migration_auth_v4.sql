-- =====================================================
-- 认证模块 V4 数据库迁移脚本
-- 1. 插入默认角色（ADMIN, OPERATOR, AUDITOR）
-- 2. 用户创建已迁移至 DataInitializer（通过环境变量配置密码）
--    若需手动通过 SQL 创建用户，请使用 GenerateBcryptHash 工具生成 BCrypt 哈希。
-- =====================================================

USE energy_platform;

-- 1. 插入默认角色（如果不存在）
INSERT IGNORE INTO t_role (role_name, role_code, permissions, status) VALUES
('系统管理员', 'ADMIN',    '["meter:view","meter:edit","meter:delete","customer:view","customer:edit","customer:delete","report:view","report:export","user:manage","role:manage","system:config"]', 1),
('运营人员',   'OPERATOR', '["meter:view","meter:edit","customer:view","customer:edit","report:view","report:export"]', 1),
('审计员',     'AUDITOR',  '["meter:view","customer:view","report:view","report:export","log:view"]', 1);

-- 2. 默认用户创建已迁移至 DataInitializer（通过环境变量配置密码）。
--    若需手动通过 SQL 创建用户，请使用 GenerateBcryptHash 工具生成 BCrypt 哈希后执行：
--    INSERT IGNORE INTO t_user (username, password, real_name, phone, role, status) VALUES
--    ('admin',    '<BCRYPT_HASH>', '系统管理员', '13800000000', 'ADMIN',    1),
--    ('operator', '<BCRYPT_HASH>', '运营小王',   '13800000001', 'OPERATOR', 1);

-- 3. 确认迁移结果
SELECT 'Migration Auth V4 completed' AS status;
SELECT role_code, role_name FROM t_role;
