-- =====================================================
-- 迁移：t_meter 新增 meter_type 列
-- 说明：为表计表添加显式的表计大类字段（0=水表, 1=电表），
--       替代原先通过厂商字段隐式推断的方式。
-- 日期：2026-06-03
-- =====================================================

-- 1. 新增列
ALTER TABLE t_meter
    ADD COLUMN meter_type INT DEFAULT NULL COMMENT '表计大类：0-水表,1-电表'
    AFTER bjlx;

-- 2. 回填存量数据（根据表特有字段推断）
-- 水表（优先）：有 meteraddress（水表编号，电表不存在此字段）→ meter_type = 0
UPDATE t_meter SET meter_type = 0
WHERE meteraddress IS NOT NULL AND meteraddress != ''
  AND meter_type IS NULL;

-- 电表：有 db_no（电表表号，水表不存在此字段）→ meter_type = 1
UPDATE t_meter SET meter_type = 1
WHERE db_no IS NOT NULL AND db_no != ''
  AND meter_type IS NULL;

-- 兜底：既无 meteraddress 也无 db_no 的，默认按电表处理
UPDATE t_meter SET meter_type = 1
WHERE meter_type IS NULL;

-- 4. 添加索引（按类型筛选是高频操作）
ALTER TABLE t_meter
    ADD INDEX idx_meter_type (meter_type);
