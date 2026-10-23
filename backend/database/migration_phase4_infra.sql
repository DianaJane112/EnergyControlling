-- =====================================================
-- Phase 4 增量迁移：分组管理 + 表计分组字段 + 公共分摊流水类型
-- 依赖：schema.sql + migration_phase2_pricing.sql + migration_phase3_report.sql
-- =====================================================
USE energy_platform;

-- =====================================================
-- 1. 分组管理表（基础档案）
-- =====================================================
CREATE TABLE IF NOT EXISTS t_meter_group (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    group_name      VARCHAR(128)    NOT NULL                 COMMENT '分组名称',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '分组描述',
    merchant_id     BIGINT          NOT NULL                 COMMENT '关联商户ID（数据权限隔离）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_name_merchant (group_name, merchant_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组管理表';

-- =====================================================
-- 2. t_meter 新增 group_id 字段（幂等，可重复执行）
-- =====================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE table_schema = 'energy_platform'
                    AND table_name = 't_meter'
                    AND column_name = 'group_id');
SET @sql_add_col := IF(@col_exists = 0,
    'ALTER TABLE t_meter ADD COLUMN group_id BIGINT DEFAULT NULL COMMENT ''关联分组ID（外键→t_meter_group.id）'' AFTER customer_id',
    'SELECT ''COLUMN group_id already exists, skip'' AS info');
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 group_id 建立索引（幂等，可重复执行）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE table_schema = 'energy_platform'
                    AND table_name = 't_meter'
                    AND index_name = 'idx_group_id');
SET @sqlstmt := IF(@idx_exists = 0,
    'CREATE INDEX idx_group_id ON t_meter(group_id)',
    'SELECT ''INDEX idx_group_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3. t_account_flow.trade_type 扩展：新增公共分摊(7)
--    1-充值, 2-消费扣费, 3-退费, 4-调账, 5-人工缴费, 6-余额清零, 7-公共分摊
-- =====================================================
ALTER TABLE t_account_flow
    MODIFY COLUMN trade_type TINYINT NOT NULL COMMENT '交易类型：1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零,7-公共分摊';
