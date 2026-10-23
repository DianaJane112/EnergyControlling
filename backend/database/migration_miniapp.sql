-- ============================================================
-- 小程序后端 - 数据库迁移脚本
-- 创建日期: 2026-06-11
-- 说明: 新增小程序用户、充值申请、票据三张表
-- ============================================================

-- 1. 小程序用户表
CREATE TABLE IF NOT EXISTS t_miniapp_user (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    openid           VARCHAR(128)    DEFAULT NULL             COMMENT '微信OpenID',
    unionid          VARCHAR(128)    DEFAULT NULL             COMMENT '微信UnionID',
    phone            VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    customer_id      BIGINT          DEFAULT NULL             COMMENT '绑定的商户ID (FK→t_customer.id，首次登录待授权时可为空)',
    nickname         VARCHAR(64)     DEFAULT NULL             COMMENT '用户昵称',
    avatar_url       VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL',
    login_type       TINYINT         NOT NULL DEFAULT 0       COMMENT '登录方式: 0=微信登录, 1=手机号登录',
    status           TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 0=禁用, 1=正常',
    last_login_time  DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    token_version    INT             NOT NULL DEFAULT 1       COMMENT 'JWT令牌版本',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted       TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    UNIQUE KEY uk_phone (phone),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户表';

-- 2. 小程序充值申请表
CREATE TABLE IF NOT EXISTS t_recharge_request (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    request_no        VARCHAR(32)     NOT NULL                 COMMENT '申请编号 (格式: MPR+时间戳+UUID前8位)',
    user_id           BIGINT          NOT NULL                 COMMENT '小程序用户ID (FK→t_miniapp_user.id)',
    customer_id       BIGINT          NOT NULL                 COMMENT '商户ID (FK→t_customer.id)',
    meter_id          BIGINT          NOT NULL                 COMMENT '表计ID (FK→t_meter.id)',
    amount            DECIMAL(18,4)   NOT NULL                 COMMENT '充值金额',
    pay_method        VARCHAR(32)     NOT NULL DEFAULT 'VIRTUAL' COMMENT '支付方式 (固定VIRTUAL)',
    audit_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING=待审核, APPROVED=已通过, REJECTED=未通过',
    audit_remark      VARCHAR(255)    DEFAULT NULL             COMMENT '审核备注/拒绝原因',
    audit_user_id     BIGINT          DEFAULT NULL             COMMENT '审核人ID (FK→t_user.id)',
    audit_time        DATETIME        DEFAULT NULL             COMMENT '审核时间',
    related_order_no  VARCHAR(64)     DEFAULT NULL             COMMENT '关联充值订单号 (FK→t_recharge_order.order_no, 审核通过后生成)',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_no (request_no),
    INDEX idx_user_id (user_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_meter_id (meter_id),
    INDEX idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序充值申请表';

-- 3. 票据表
CREATE TABLE IF NOT EXISTS t_invoice (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    invoice_no        VARCHAR(32)     NOT NULL                 COMMENT '票据编号 (格式: INV+时间戳+UUID前8位)',
    customer_id       BIGINT          NOT NULL                 COMMENT '商户ID (FK→t_customer.id)',
    meter_id          BIGINT          DEFAULT NULL             COMMENT '表计ID (FK→t_meter.id)',
    recharge_order_id BIGINT          DEFAULT NULL             COMMENT '关联充值订单ID (FK→t_recharge_order.id)',
    amount            DECIMAL(18,4)   NOT NULL                 COMMENT '票据金额',
    invoice_type      VARCHAR(32)     NOT NULL DEFAULT 'ELECTRIC' COMMENT '票据类型: ELECTRIC=电费, WATER=水费',
    invoice_status    VARCHAR(32)     NOT NULL DEFAULT 'NOT_ISSUED' COMMENT '开票状态: NOT_ISSUED=未开票, ISSUED=已开票',
    issue_time        DATETIME        DEFAULT NULL             COMMENT '开票时间',
    remark            VARCHAR(255)    DEFAULT NULL             COMMENT '备注',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (invoice_no),
    INDEX idx_customer_id (customer_id),
    INDEX idx_meter_id (meter_id),
    INDEX idx_invoice_status (invoice_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据表';
