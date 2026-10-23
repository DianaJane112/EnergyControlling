-- =====================================================
-- Phase 3 增量迁移：用电量日报表（预聚合表）
-- 每天凌晨 1:00 由 ElectricityReportScheduler 定时任务写入
-- =====================================================
USE energy_platform;

-- =====================================================
-- 19. 用电量日报表（预聚合，避免全表扫描 t_meter_reading）
-- =====================================================
CREATE TABLE IF NOT EXISTS t_report_daily_electricity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    report_date         DATE            NOT NULL                 COMMENT '报表日期（关联的用电日）',
    meter_id            BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id         BIGINT          DEFAULT NULL             COMMENT '商户ID（冗余，便于按商户筛选）',
    group_id            BIGINT          DEFAULT NULL             COMMENT '分组ID（冗余，便于按分组筛选，0=未分组）',
    electricity_consumed DECIMAL(18,4)  NOT NULL DEFAULT 0.0000  COMMENT '当日用电量（kWh），= 当日23:59:59读数 - 前日23:59:59读数',
    peak_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '尖时段用电量（kWh）',
    busy_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '峰时段用电量（kWh）',
    normal_consumed     DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '平时段用电量（kWh）',
    idle_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '谷时段用电量（kWh）',
    charge_amount       DECIMAL(14,4)   DEFAULT 0.0000           COMMENT '当日扣费金额（元）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_date (meter_id, report_date),
    KEY idx_report_date (report_date),
    KEY idx_customer_id (customer_id),
    KEY idx_group_id (group_id),
    KEY idx_meter_id (meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用电量日报表（每天凌晨预聚合）';
