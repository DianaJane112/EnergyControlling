-- =====================================================
-- 昊普电表集成 - 添加通信参数 + 电表编码字段
-- 迁移日期: 2026-06-16
-- =====================================================
USE energy_platform;

-- 1. 昊普电表唯一设备编号（对应昊普接口中的 meterCode）
ALTER TABLE t_meter
    ADD COLUMN meterCode BIGINT DEFAULT NULL COMMENT '昊普电表唯一设备编号';

-- 2. 采集器编号（兼容多个厂商，存储为字符串）
ALTER TABLE t_meter
    ADD COLUMN collectorCode VARCHAR(64) DEFAULT NULL COMMENT '采集器唯一编码';

-- 3. 串口波特率（项目默认 B2400）
ALTER TABLE t_meter
    ADD COLUMN bpsType VARCHAR(16) DEFAULT 'B2400' COMMENT '串口波特率：B600/B1200/B2400/B4800/B7200/B9600/B19200';

-- 4. 串口校验位（项目固定 EVEN）
ALTER TABLE t_meter
    ADD COLUMN bitType VARCHAR(8) DEFAULT 'EVEN' COMMENT '串口校验位：NONE/ODD/EVEN';

-- 5. 电表通信协议类型（项目固定 DLT645_2007）
ALTER TABLE t_meter
    ADD COLUMN protoType VARCHAR(32) DEFAULT 'DLT645_2007' COMMENT '通信协议：NONE/DLT645_1997/AC_SAMPLING/DLT645_2007/CHINA_TOWER/CJT188_2004';

-- 6. 数据序号（采集器下电表配置排序）
ALTER TABLE t_meter
    ADD COLUMN serial INT DEFAULT NULL COMMENT '数据序号，用于列表排序';

-- 7. 电表设备类型
ALTER TABLE t_meter
    ADD COLUMN deviceType VARCHAR(16) DEFAULT NULL COMMENT '设备类型：DC_1(单相直流)/AC_1(单相交流)/AC_3(三相交流)';

-- 8. 添加 meterCode 唯一索引（同一采集器下 meterCode 唯一）
ALTER TABLE t_meter
    ADD INDEX idx_meter_code (meterCode),
    ADD INDEX idx_collector_code (collectorCode);
