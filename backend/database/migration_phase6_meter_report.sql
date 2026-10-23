-- =====================================================
-- Phase 6 增量迁移：表计综合报表（水+电统一预聚合）
-- 每天凌晨 2:00 由 MeterReportScheduler 定时任务写入
-- 覆盖：日报表、月报表、已删除电表月报查询
-- =====================================================
USE energy_platform;

-- =====================================================
-- 1. 表计日报表（水+电统一，每天凌晨预聚合）
-- 对应需求：用电量/用水量日报表（§报表统计）
-- =====================================================
CREATE TABLE IF NOT EXISTS tb_meter_daily_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          NOT NULL                 COMMENT '项目ID（关联t_project.id）',
    group_id        BIGINT          DEFAULT NULL             COMMENT '分组ID（关联t_meter_group.id）',
    merchant_id     BIGINT          DEFAULT NULL             COMMENT '商户ID（关联t_customer.id）',
    device_no       VARCHAR(64)     NOT NULL                 COMMENT '设备号（优先db_no，其次steal_no，最后meteraddress）',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID（关联t_meter.id）',
    meter_type      VARCHAR(16)     NOT NULL DEFAULT 'ELECTRIC' COMMENT '表计类型：ELECTRIC-电表，WATER-水表',
    total_usage     DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '总用量（电：kWh，水：吨）',
    peak_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '尖时段用量',
    busy_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '峰时段用量',
    normal_usage    DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '平时段用量',
    idle_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '谷时段用量',
    stat_date       DATE            NOT NULL                 COMMENT '统计日期',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_date (meter_id, stat_date),
    KEY idx_project_id (project_id),
    KEY idx_group_id (group_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_device_no (device_no),
    KEY idx_stat_date (stat_date),
    KEY idx_meter_type (meter_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计日报表（水+电统一，凌晨2:00预聚合）';

-- =====================================================
-- 2. 表计月报表（由 MeterReportScheduler 在每月最后一天生成）
-- 对应需求：用电量/用水量月报表（§报表统计）
-- =====================================================
CREATE TABLE IF NOT EXISTS tb_meter_monthly_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          NOT NULL                 COMMENT '项目ID（关联t_project.id）',
    group_id        BIGINT          DEFAULT NULL             COMMENT '分组ID（关联t_meter_group.id）',
    merchant_id     BIGINT          DEFAULT NULL             COMMENT '商户ID（关联t_customer.id）',
    device_no       VARCHAR(64)     NOT NULL                 COMMENT '设备号',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID（关联t_meter.id）',
    meter_type      VARCHAR(16)     NOT NULL DEFAULT 'ELECTRIC' COMMENT '表计类型：ELECTRIC-电表，WATER-水表',
    total_usage     DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '月度总用量',
    peak_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '尖时段月度用量',
    busy_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '峰时段月度用量',
    normal_usage    DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '平时段月度用量',
    idle_usage      DECIMAL(14,4)   NOT NULL DEFAULT 0.0000  COMMENT '谷时段月度用量',
    forward_start_reading  DECIMAL(18,4)   DEFAULT NULL       COMMENT '正向有功上期示数',
    forward_start_time     DATETIME        DEFAULT NULL       COMMENT '正向有功上期时间',
    forward_end_reading    DECIMAL(18,4)   DEFAULT NULL       COMMENT '正向有功本期示数',
    forward_end_time       DATETIME        DEFAULT NULL       COMMENT '正向有功本期时间',
    forward_usage          DECIMAL(18,4)   DEFAULT NULL       COMMENT '正向有功用量',
    reverse_start_reading  DECIMAL(18,4)   DEFAULT NULL       COMMENT '反向有功上期示数',
    reverse_start_time     DATETIME        DEFAULT NULL       COMMENT '反向有功上期时间',
    reverse_end_reading    DECIMAL(18,4)   DEFAULT NULL       COMMENT '反向有功本期示数',
    reverse_end_time       DATETIME        DEFAULT NULL       COMMENT '反向有功本期时间',
    reverse_usage          DECIMAL(18,4)   DEFAULT NULL       COMMENT '反向有功用量',
    reverse_ratio          DECIMAL(10,4)   DEFAULT NULL       COMMENT '反向占比(%)',
    total_start_reading    DECIMAL(18,4)   DEFAULT NULL       COMMENT '总起示数',
    total_end_reading      DECIMAL(18,4)   DEFAULT NULL       COMMENT '总止示数',
    previous_balance       DECIMAL(18,4)   DEFAULT NULL       COMMENT '上期余额',
    current_balance        DECIMAL(18,4)   DEFAULT NULL       COMMENT '本期余额',
    data_update_time       DATETIME        DEFAULT NULL       COMMENT '数据更新时间',
    stat_month      VARCHAR(7)      NOT NULL                 COMMENT '统计月份（格式：YYYY-MM）',
    stat_year       VARCHAR(4)      NOT NULL                 COMMENT '统计年份（冗余，加速年报表查询）',
    daily_count     INT             NOT NULL DEFAULT 0       COMMENT '本月有效日报天数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_month (meter_id, stat_month),
    KEY idx_project_id (project_id),
    KEY idx_group_id (group_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_device_no (device_no),
    KEY idx_stat_month (stat_month),
    KEY idx_stat_year (stat_year),
    KEY idx_meter_type (meter_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计月报表（水+电统一，月末聚合）';

-- =====================================================
-- 说明：月报表数据来源
--   - MeterReportScheduler 在每天凌晨聚合日报后，检查是否为当月最后一天
--   - 若是月末，将当月所有日报数据 SUM 聚合写入月报表
--   - 已删除电表的月报查询：直接 JOIN t_meter（含 is_deleted=1）与
--     tb_meter_monthly_report，不做逻辑删除过滤，确保财务可查
-- =====================================================
