-- 商户型虚拟表兼容迁移：历史总表-分表关系保持不变，新增 MERCHANT 模式允许无父表
ALTER TABLE t_shadow_meter
  MODIFY COLUMN parent_meter_id BIGINT NULL COMMENT '父表计ID，商户型虚拟表为空';
