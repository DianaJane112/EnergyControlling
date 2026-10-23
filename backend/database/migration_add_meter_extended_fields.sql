-- =====================================================
-- 表计管理功能增强 - 字段迁移脚本
-- 添加：CT变比、PT变比、标记状态、启用时间、停用警告标志、通讯状态描述
-- 扩展 bjlx 支持：分摊表(5)、公区总表(6)
-- 执行日期：2026-06-03
-- =====================================================
USE energy_platform;

-- 1. 添加 CT变比（电流互感器变比）
ALTER TABLE t_meter
    ADD COLUMN ct_ratio DECIMAL(10,4) DEFAULT NULL COMMENT 'CT变比（电流互感器变比，如200:5=40）',
    ADD COLUMN pt_ratio DECIMAL(10,4) DEFAULT NULL COMMENT 'PT变比（电压互感器变比）';

-- 2. 添加标记状态（用于标记/标注表计，如：正常、关注、异常、待维护）
ALTER TABLE t_meter
    ADD COLUMN mark_status VARCHAR(16) DEFAULT 'NORMAL' COMMENT '标记状态：NORMAL-正常, ATTENTION-关注, ABNORMAL-异常, MAINTENANCE-待维护';

-- 3. 添加启用时间（记录表计正式投入使用的日期时间）
ALTER TABLE t_meter
    ADD COLUMN enabled_time DATETIME DEFAULT NULL COMMENT '启用时间（表计正式投入使用时间）';

-- 4. 添加停用警告开关（0-启用告警, 1-停用告警）
ALTER TABLE t_meter
    ADD COLUMN disable_warning TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否停用警告：0-启用告警, 1-停用告警';

-- 5. 添加通讯状态详细描述
ALTER TABLE t_meter
    ADD COLUMN comm_status_desc VARCHAR(64) DEFAULT NULL COMMENT '通讯状态描述：正常/异常/离线/信号弱';

-- 6. 添加索引
ALTER TABLE t_meter
    ADD INDEX idx_mark_status (mark_status),
    ADD INDEX idx_enabled_time (enabled_time);
