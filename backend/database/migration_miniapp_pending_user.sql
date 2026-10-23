-- 小程序首次微信登录先登记用户，再由管理员绑定账户或授予虚拟表查看权限。
-- 幂等执行：重复执行 MODIFY 不会丢失数据。
ALTER TABLE t_miniapp_user
    MODIFY COLUMN customer_id BIGINT NULL
    COMMENT '绑定的商户ID；首次登录待授权时可为空';
