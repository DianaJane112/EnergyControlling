-- =====================================================
-- 计费闭环 V1 数据库迁移脚本
-- 1. t_customer 增加 balance 余额字段
-- 2. 创建 t_account_flow 资金流水/对账表
-- =====================================================

USE energy_platform;

-- 1. 商户表增加余额字段
ALTER TABLE t_customer
    ADD COLUMN balance DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '商户账户余额（元）'
    AFTER remark;

-- 2. 资金流水/对账表
CREATE TABLE IF NOT EXISTS t_account_flow (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_no         VARCHAR(64)     NOT NULL                 COMMENT '流水号（UUID全局唯一）',
    customer_id     BIGINT          NOT NULL                 COMMENT '关联商户ID',
    meter_id        BIGINT          DEFAULT NULL             COMMENT '关联表计ID（可选）',
    balance_before  DECIMAL(18,4)   NOT NULL                 COMMENT '变动前余额',
    change_amount   DECIMAL(18,4)   NOT NULL                 COMMENT '变动金额（正=入账，负=出账）',
    balance_after   DECIMAL(18,4)   NOT NULL                 COMMENT '变动后余额',
    trade_type      TINYINT         NOT NULL                 COMMENT '交易类型：1-充值，2-消费扣费，3-退费，4-调账',
    biz_no          VARCHAR(64)     NOT NULL                 COMMENT '关联业务单号（充值订单号/退费编号/调整编号）',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作人',
    operate_time    DATETIME        NOT NULL                 COMMENT '操作时间',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注/原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_no (flow_no),
    KEY idx_customer_id (customer_id),
    KEY idx_biz_no (biz_no),
    KEY idx_trade_type (trade_type),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水/对账表';
