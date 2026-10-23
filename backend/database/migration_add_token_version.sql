-- =============================================================================
-- 迁移：为 t_user / t_miniapp_user 增加 token_version 字段
-- 用途：JWT Token 废止机制 —— 修改密码或登出时 +1，全局废止旧 Token
-- =============================================================================
ALTER TABLE t_user ADD COLUMN token_version INT NOT NULL DEFAULT 1
    COMMENT 'Token版本号：修改密码或登出时+1，用于全局废止旧Token';

ALTER TABLE t_miniapp_user ADD COLUMN token_version INT NOT NULL DEFAULT 1
    COMMENT 'Token版本号：登出/账号变更时+1，用于全局废止旧Token';
