-- =====================================================
-- Phase 2 增量迁移：复费率策略 + 人工退费日志 + 表计字段补盲
-- 依赖：schema.sql（Phase 1 基线）
-- =====================================================
USE energy_platform;

-- =====================================================
-- 1. 费率时段策略表（复费率：尖峰平谷时段+电价配置）
-- =====================================================
CREATE TABLE IF NOT EXISTS t_price_strategy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    strategy_name       VARCHAR(128)    NOT NULL                 COMMENT '策略名称（如"夏季工商业分时电价"）',
    strategy_code       VARCHAR(64)     NOT NULL                 COMMENT '策略编码（唯一）',
    project_id          BIGINT          DEFAULT NULL             COMMENT '关联项目ID（NULL=全局策略）',
    meter_type         TINYINT         NOT NULL DEFAULT 1       COMMENT '表计类型：0-水表，1-电表',
    price_type          INT             NOT NULL DEFAULT 2       COMMENT '策略类型：1-固定单价, 2-分时电价(尖峰平谷), 3-阶梯电价',
    time_slots          JSON            DEFAULT NULL             COMMENT '时段电价JSON：[{"slotName":"尖","startHour":19,"endHour":21,"price":1.20},{...}]',
    fixed_price         DECIMAL(10,4)   DEFAULT NULL             COMMENT '固定单价（price_type=1时使用）',
    plus_price          DECIMAL(10,4)   DEFAULT 0.0000           COMMENT '附加费（元/kWh）',
    effective_date      DATE            NOT NULL                 COMMENT '生效日期',
    expire_date         DATE            DEFAULT NULL             COMMENT '失效日期（NULL=永久有效）',
    status              TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用, 1-启用',
    remark              VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_code (strategy_code),
    KEY idx_project_id (project_id),
    KEY idx_meter_type (meter_type),
    KEY idx_price_type (price_type),
    KEY idx_effective_date (effective_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费率时段策略表（支持尖峰平谷分时电价）';

-- =====================================================
-- 2. 人工退费日志表（审计核心，每笔人工退费必写一条）
-- =====================================================
CREATE TABLE IF NOT EXISTS t_refund_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_no             VARCHAR(64)     NOT NULL                 COMMENT '流水号（全局唯一，UUID生成）',
    meter_id            BIGINT          NOT NULL                 COMMENT '关联表计ID',
    customer_id         BIGINT          NOT NULL                 COMMENT '关联商户ID',
    balance_before      DECIMAL(18,4)   NOT NULL                 COMMENT '退费前余额（元）',
    refund_amount       DECIMAL(18,4)   NOT NULL                 COMMENT '退费金额（元，正数）',
    balance_after       DECIMAL(18,4)   NOT NULL                 COMMENT '退费后余额（元）',
    operator_id         BIGINT          NOT NULL                 COMMENT '操作员ID（关联t_user.id）',
    operator_name       VARCHAR(64)     DEFAULT NULL             COMMENT '操作员姓名（冗余）',
    refund_reason       VARCHAR(512)    NOT NULL                 COMMENT '退费原因（必填）',
    operate_time        DATETIME        NOT NULL                 COMMENT '操作时间',
    remark              VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_no (flow_no),
    KEY idx_meter_id (meter_id),
    KEY idx_customer_id (customer_id),
    KEY idx_operator_id (operator_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工退费日志表';

-- =====================================================
-- 3. t_meter 表字段补盲：新增费率策略外键（幂等，可重复执行）
-- =====================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE table_schema = 'energy_platform'
                    AND table_name = 't_meter'
                    AND column_name = 'rate_config_id');
SET @sql_add_col := IF(@col_exists = 0,
    'ALTER TABLE t_meter ADD COLUMN rate_config_id BIGINT DEFAULT NULL COMMENT ''关联费率策略ID（外键→t_price_strategy.id）'' AFTER last_settled_time',
    'SELECT ''COLUMN rate_config_id already exists, skip'' AS info');
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 rate_config_id 建立索引（幂等，可重复执行）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE table_schema = 'energy_platform'
                    AND table_name = 't_meter'
                    AND index_name = 'idx_rate_config_id');
SET @sqlstmt := IF(@idx_exists = 0,
    'CREATE INDEX idx_rate_config_id ON t_meter(rate_config_id)',
    'SELECT ''INDEX idx_rate_config_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 4. 收费业务流水类型扩展：新增人工缴费(5)、余额清零(6)
--    t_account_flow.trade_type 枚举语义扩展（注释级，不影响已有数据）
--    1-充值, 2-消费扣费, 3-退费, 4-调账, 5-人工缴费, 6-余额清零
-- =====================================================
ALTER TABLE t_account_flow
    MODIFY COLUMN trade_type TINYINT NOT NULL COMMENT '交易类型：1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零';

-- =====================================================
-- 5. 插入默认费率策略（固定电价0.85元/kWh，作为全局兜底）
-- =====================================================
INSERT IGNORE INTO t_price_strategy (strategy_name, strategy_code, price_type, fixed_price, plus_price, effective_date, status, remark)
VALUES ('默认固定电价', 'DEFAULT_FIXED', 1, 0.8500, 0.0000, CURDATE(), 1, '系统默认全局固定电价策略');
