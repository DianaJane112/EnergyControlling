-- 电表厂商对接配置表
CREATE TABLE IF NOT EXISTS t_electric_meter_vendor (
    id                 BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    vendor_name        VARCHAR(128) NOT NULL                 COMMENT '电表厂商名称',
    api_url            VARCHAR(512) NOT NULL                 COMMENT 'API基础地址',
    app_id             VARCHAR(128) NOT NULL                 COMMENT 'AppId',
    app_secret         VARCHAR(256) NOT NULL                 COMMENT 'AppSecret',
    webhook_signature  VARCHAR(256) DEFAULT NULL             COMMENT 'Webhook签名密钥',
    status             TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    last_enabled_time  DATETIME     DEFAULT NULL             COMMENT '最近一次启用时间',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          VARCHAR(64)  DEFAULT NULL             COMMENT '创建人',
    is_deleted         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_vendor_name (vendor_name),
    KEY idx_status (status),
    KEY idx_last_enabled_time (last_enabled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电表厂商对接配置表';
