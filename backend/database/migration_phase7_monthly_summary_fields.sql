-- =====================================================
-- Phase 7 增量迁移：月度用电量汇总表补充字段
-- 为 tb_meter_monthly_report 添加正向/反向有功电量、
-- 总起止示数、余额、数据更新时间等字段
-- =====================================================
USE energy_platform;

ALTER TABLE tb_meter_monthly_report
    ADD COLUMN forward_start_reading  DECIMAL(18,4)   DEFAULT NULL   COMMENT '正向有功上期示数'       AFTER idle_usage,
    ADD COLUMN forward_start_time     DATETIME        DEFAULT NULL   COMMENT '正向有功上期时间'       AFTER forward_start_reading,
    ADD COLUMN forward_end_reading    DECIMAL(18,4)   DEFAULT NULL   COMMENT '正向有功本期示数'       AFTER forward_start_time,
    ADD COLUMN forward_end_time       DATETIME        DEFAULT NULL   COMMENT '正向有功本期时间'       AFTER forward_end_reading,
    ADD COLUMN forward_usage          DECIMAL(18,4)   DEFAULT NULL   COMMENT '正向有功用量'           AFTER forward_end_time,
    ADD COLUMN reverse_start_reading  DECIMAL(18,4)   DEFAULT NULL   COMMENT '反向有功上期示数'       AFTER forward_usage,
    ADD COLUMN reverse_start_time     DATETIME        DEFAULT NULL   COMMENT '反向有功上期时间'       AFTER reverse_start_reading,
    ADD COLUMN reverse_end_reading    DECIMAL(18,4)   DEFAULT NULL   COMMENT '反向有功本期示数'       AFTER reverse_start_time,
    ADD COLUMN reverse_end_time       DATETIME        DEFAULT NULL   COMMENT '反向有功本期时间'       AFTER reverse_end_reading,
    ADD COLUMN reverse_usage          DECIMAL(18,4)   DEFAULT NULL   COMMENT '反向有功用量'           AFTER reverse_end_time,
    ADD COLUMN reverse_ratio          DECIMAL(10,4)   DEFAULT NULL   COMMENT '反向占比(%)'            AFTER reverse_usage,
    ADD COLUMN total_start_reading    DECIMAL(18,4)   DEFAULT NULL   COMMENT '总起示数'               AFTER reverse_ratio,
    ADD COLUMN total_end_reading      DECIMAL(18,4)   DEFAULT NULL   COMMENT '总止示数'               AFTER total_start_reading,
    ADD COLUMN previous_balance       DECIMAL(18,4)   DEFAULT NULL   COMMENT '上期余额'               AFTER total_end_reading,
    ADD COLUMN current_balance        DECIMAL(18,4)   DEFAULT NULL   COMMENT '本期余额'               AFTER previous_balance,
    ADD COLUMN data_update_time       DATETIME        DEFAULT NULL   COMMENT '数据更新时间'           AFTER current_balance;

-- 说明：
--   这些字段由定时任务 MeterReportScheduler 在月末聚合时一并填充，
--   数据来源为每日冻结表 t_daily_freeze_record 的起止读数和
--   t_customer 的余额快照。
-- =====================================================
