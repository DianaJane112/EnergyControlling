-- =====================================================
-- Energy Controller Platform — 生产环境一键部署脚本
-- =====================================================
-- 版本：v1.0.0
-- 日期：2026-06-16
-- 说明：本脚本合并了 schema.sql + 全部 migration 脚本，
--        每个 CREATE TABLE 包含该表的最终完整字段集，
--        可一键部署到空白数据库，无需逐文件执行。
-- 执行方式：mysql -u root -p < deploy_schema.sql
-- =====================================================

-- ═══════════════════════════════════════════════════════════
-- 步骤 0：创建数据库（如果不存在）
-- ═══════════════════════════════════════════════════════════
CREATE DATABASE IF NOT EXISTS energy_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE energy_platform;

CREATE TABLE IF NOT EXISTS t_billing_segment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(64) NOT NULL, virtual_meter_id BIGINT NOT NULL,
    source_meter_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, billing_date DATE NOT NULL,
    period_start DATETIME NULL, period_end DATETIME NOT NULL,
    previous_reading DECIMAL(20,6), current_reading DECIMAL(20,6),
    raw_consumption DECIMAL(20,6) NOT NULL, billed_consumption DECIMAL(20,6) NOT NULL,
    ct_ratio DECIMAL(12,6), pt_ratio DECIMAL(12,6), strategy_id BIGINT NOT NULL,
    strategy_code VARCHAR(64), strategy_version INT, price_type INT,
    unit_price DECIMAL(18,8), plus_price DECIMAL(18,8), charge_amount DECIMAL(18,4) NOT NULL,
    project_id BIGINT, project_name VARCHAR(128), area_group_id BIGINT, area_name VARCHAR(128),
    category_group_id BIGINT, category_name VARCHAR(128), detail_json JSON,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_billing_segment_period(billing_date,customer_id),
    INDEX idx_billing_segment_dimension(project_id,area_group_id,category_group_id),
    INDEX idx_billing_segment_meter(virtual_meter_id,source_meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变计费分段与维度快照';



CREATE TABLE IF NOT EXISTS t_virtual_meter_viewer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    virtual_meter_id BIGINT NOT NULL,
    miniapp_user_id BIGINT NOT NULL,
    permission_type VARCHAR(16) NOT NULL DEFAULT 'VIEW',
    status TINYINT NOT NULL DEFAULT 1,
    effective_time DATETIME DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_virtual_meter_viewer (virtual_meter_id, miniapp_user_id),
    INDEX idx_virtual_meter_viewer_user (miniapp_user_id),
    INDEX idx_virtual_meter_viewer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟表查看用户授权';


-- ============================================================
-- 一、核心档案表（用户 / 角色 / 项目 / 商户 / 表计 / 采集器）
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username        VARCHAR(64)     NOT NULL                 COMMENT '用户名（唯一）',
    password        VARCHAR(256)    NOT NULL                 COMMENT 'BCrypt 加密密码',
    real_name       VARCHAR(64)     DEFAULT NULL             COMMENT '真实姓名',
    phone           VARCHAR(32)     DEFAULT NULL             COMMENT '手机号',
    role            VARCHAR(32)     NOT NULL DEFAULT 'OPERATOR' COMMENT '角色编码：ADMIN/OPERATOR/AUDITOR',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_name       VARCHAR(64)     NOT NULL                 COMMENT '角色名称',
    role_code       VARCHAR(64)     NOT NULL                 COMMENT '角色编码（唯一）',
    permissions     JSON            DEFAULT NULL             COMMENT '权限列表 JSON',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 项目表
CREATE TABLE IF NOT EXISTS t_project (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_name    VARCHAR(128)    NOT NULL                 COMMENT '项目名称',
    project_code    VARCHAR(64)     DEFAULT NULL             COMMENT '项目编码',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '项目描述',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_name (project_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 4. 商户档案表（已含 balance 字段）
CREATE TABLE IF NOT EXISTS t_customer (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_code   VARCHAR(64)     NOT NULL                 COMMENT '商户编号',
    customer_name   VARCHAR(128)    NOT NULL                 COMMENT '商户名称',
    area_name       VARCHAR(128)    DEFAULT NULL             COMMENT '区域名称',
    address         VARCHAR(256)    DEFAULT NULL             COMMENT '地址',
    phone           VARCHAR(32)     DEFAULT NULL             COMMENT '联系电话',
    customer_type   VARCHAR(32)     DEFAULT NULL             COMMENT '商户类型',
    use_nature      VARCHAR(32)     DEFAULT NULL             COMMENT '用电性质',
    balance         DECIMAL(18,4)   NOT NULL DEFAULT 0.0000  COMMENT '账户余额（元）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户档案表';

-- 5. 表计主表（完整字段：含计费、通信、标记、昊普等所有扩展字段）
CREATE TABLE IF NOT EXISTS t_meter (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id         BIGINT          DEFAULT NULL             COMMENT '关联客户ID',
    meter_kind          VARCHAR(16)     NOT NULL DEFAULT 'PHYSICAL' COMMENT '表计性质：PHYSICAL-真实表，VIRTUAL-虚拟结算表',
    group_id            BIGINT          DEFAULT NULL             COMMENT '关联分组ID',
    steal_no            VARCHAR(64)     DEFAULT NULL             COMMENT '表钢印号/表通讯地址',
    vender              VARCHAR(64)     DEFAULT NULL             COMMENT '厂商代码',
    meter_type_name     VARCHAR(64)     DEFAULT NULL             COMMENT '表类型名称',
    caliber             VARCHAR(16)     DEFAULT NULL             COMMENT '表口径',
    switch_op           VARCHAR(4)      DEFAULT NULL             COMMENT '阀控标识',
    start_val           VARCHAR(32)     DEFAULT NULL             COMMENT '新表底度',
    terminal_name       VARCHAR(128)    DEFAULT NULL             COMMENT '终端名称/集中器名称',
    channel_type        VARCHAR(8)      DEFAULT NULL             COMMENT '表通道类型',
    meter_phone         VARCHAR(32)     DEFAULT NULL             COMMENT '表计电话',
    telecomm_sn         VARCHAR(32)     DEFAULT NULL             COMMENT '通讯序号',
    imei                VARCHAR(32)     DEFAULT NULL             COMMENT 'IMEI码',
    iccid               VARCHAR(32)     DEFAULT NULL             COMMENT 'ICCID/IMSI码',
    address             VARCHAR(256)    DEFAULT NULL             COMMENT '表计安装地址',
    create_date         DATETIME        DEFAULT NULL             COMMENT '建档时间',
    type                VARCHAR(4)      DEFAULT NULL             COMMENT '建档类型',
    sign                VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    db_no               VARCHAR(12)     DEFAULT NULL             COMMENT '表号/电表表号12位字符串',
    name                VARCHAR(128)    DEFAULT NULL             COMMENT '商户名',
    bei                 INT             DEFAULT 1                COMMENT '倍率',
    bjlx                INT             DEFAULT NULL             COMMENT '表计类型：1-租户,2-总表,3-公共,4-其他',
    meter_type          INT             DEFAULT NULL             COMMENT '表计大类：0-水表,1-电表',
    credit              DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '透支额度',
    warnline            DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '告警额度',
    creatuser           VARCHAR(64)     DEFAULT NULL             COMMENT '安装人',
    cttime              VARCHAR(32)     DEFAULT NULL             COMMENT '安装时间',
    feetype             INT             DEFAULT 1                COMMENT '付费方式：1-预付费,2-后付费',
    pro_id              BIGINT          DEFAULT NULL             COMMENT '项目ID',
    pro_name            VARCHAR(128)    DEFAULT NULL             COMMENT '项目名称',
    plus_price          DECIMAL(10,4)   DEFAULT 0.0000           COMMENT '附加电费',
    price_type          INT             DEFAULT 1                COMMENT '电价类型',
    price_content       JSON            DEFAULT NULL             COMMENT '电价内容JSON',
    join_phone          JSON            DEFAULT NULL             COMMENT '关联用户电话',
    collector_device_no VARCHAR(64)     DEFAULT NULL             COMMENT '采集器号',
    phase               INT             DEFAULT NULL             COMMENT '相数',
    meteraddress        VARCHAR(64)     DEFAULT NULL             COMMENT '水表编号',
    companycode         VARCHAR(64)     DEFAULT NULL             COMMENT '厂家编号',
    water_vendor_id     BIGINT          DEFAULT NULL             COMMENT '水表厂商配置ID',
    customerno          VARCHAR(64)     DEFAULT NULL             COMMENT '用户编号/营收户号',
    meter_caliber       VARCHAR(16)     DEFAULT NULL             COMMENT '水表口径',
    controlling         INT             DEFAULT NULL             COMMENT '是否有阀控',
    state_valve         INT             DEFAULT NULL             COMMENT '阀门状态',
    signal_strength     INT             DEFAULT NULL             COMMENT '信号强度',
    battery_voltage     DECIMAL(10,4)   DEFAULT NULL             COMMENT '电池电压',
    status              INT             DEFAULT 1                COMMENT '运行状态',
    online              INT             DEFAULT 1                COMMENT '在线状态',
    meter_signal        INT             DEFAULT NULL             COMMENT '信号强度0-30',
    -- 计费追踪（来自 migration_auto_billing_v2）
    last_settled_reading DECIMAL(18,4)  DEFAULT NULL             COMMENT '上次结算读数',
    last_settled_time    DATETIME        DEFAULT NULL             COMMENT '上次结算时间',
    rate_config_id       BIGINT          DEFAULT NULL             COMMENT '关联费率策略ID',
    -- 变比参数（来自 migration_add_meter_extended_fields）
    ct_ratio            DECIMAL(10,4)   DEFAULT NULL             COMMENT 'CT变比',
    pt_ratio            DECIMAL(10,4)   DEFAULT NULL             COMMENT 'PT变比',
    -- 标记与管理字段（来自 migration_add_meter_extended_fields）
    mark_status         VARCHAR(16)     DEFAULT 'NORMAL'         COMMENT '标记状态：NORMAL-正常, ATTENTION-关注, ABNORMAL-异常, MAINTENANCE-待维护',
    enabled_time        DATETIME        DEFAULT NULL             COMMENT '启用时间',
    disable_warning     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否停用警告',
    comm_status_desc    VARCHAR(64)     DEFAULT NULL             COMMENT '通讯状态描述',
    -- 通信参数（昊普/多厂商通用，来自 migration_haopu_meter_fields）
    collectorCode       VARCHAR(64)     DEFAULT NULL             COMMENT '采集器唯一编码',
    meterCode           BIGINT          DEFAULT NULL             COMMENT '昊普电表唯一设备编号',
    bpsType             VARCHAR(16)     DEFAULT 'B2400'          COMMENT '串口波特率',
    bitType             VARCHAR(8)      DEFAULT 'EVEN'           COMMENT '串口校验位',
    protoType           VARCHAR(32)     DEFAULT 'DLT645_2007'    COMMENT '通信协议类型',
    serial              INT             DEFAULT NULL             COMMENT '数据序号',
    deviceType          VARCHAR(16)     DEFAULT NULL             COMMENT '电表设备类型',
    -- 审计字段
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_db_no (db_no),
    KEY idx_customer_id (customer_id),
    KEY idx_group_id (group_id),
    KEY idx_status (status),
    KEY idx_meter_type (meter_type),
    KEY idx_meter_kind_type_deleted (meter_kind, meter_type, is_deleted),
    KEY idx_mark_status (mark_status),
    KEY idx_enabled_time (enabled_time),
    KEY idx_meter_code (meterCode),
    KEY idx_collector_code (collectorCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计主表';

-- 6. 表计读数记录表（含 create_time 索引）
CREATE TABLE IF NOT EXISTS t_meter_reading (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          NOT NULL                 COMMENT '关联表计ID',
    collectorCode   VARCHAR(64)     DEFAULT NULL             COMMENT '采集器编号',
    timestamp       DATETIME        NOT NULL                 COMMENT '读数时间戳',
    kwh             DECIMAL(18,4)   DEFAULT NULL             COMMENT '组合有功总电量',
    kwhPeak         DECIMAL(18,4)   DEFAULT NULL             COMMENT '尖电量',
    kwhBusy         DECIMAL(18,4)   DEFAULT NULL             COMMENT '峰电量',
    kwhNormal       DECIMAL(18,4)   DEFAULT NULL             COMMENT '平电量',
    kwhIdle         DECIMAL(18,4)   DEFAULT NULL             COMMENT '谷电量',
    dataDt          VARCHAR(32)     DEFAULT NULL             COMMENT '数据日期（yyMMddHHmmss）',
    data_value0     INT             DEFAULT NULL             COMMENT '数据值0',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_meter_id (meter_id),
    KEY idx_timestamp (timestamp),
    KEY idx_meter_time (meter_id, timestamp),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计读数记录表';

-- 7. 采集器表（含 customer_id 字段 + 索引，来自 migration_phase5_collector）
CREATE TABLE IF NOT EXISTS t_collector (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id     BIGINT          DEFAULT NULL             COMMENT '关联商户ID（平台侧权限隔离）',
    coll_code       VARCHAR(64)     NOT NULL                 COMMENT '采集器编号',
    coll_name       VARCHAR(128)    DEFAULT NULL             COMMENT '采集器名称',
    fact_code       VARCHAR(64)     DEFAULT NULL             COMMENT '厂家编码',
    location        VARCHAR(256)    DEFAULT NULL             COMMENT '安装位置',
    area_code       VARCHAR(64)     DEFAULT NULL             COMMENT '区域编码',
    memo            VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coll_code (coll_code),
    KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集器表';


-- ============================================================
-- 二、财务相关表（资金流水 / 充值 / 费率 / 账单）
-- ============================================================

-- 8. 资金流水/对账表（trade_type 已扩展到 7 种类型）
CREATE TABLE IF NOT EXISTS t_account_flow (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_no         VARCHAR(64)     NOT NULL                 COMMENT '流水号（UUID全局唯一）',
    customer_id     BIGINT          NOT NULL                 COMMENT '关联商户ID',
    meter_id        BIGINT          DEFAULT NULL             COMMENT '关联表计ID（可选）',
    balance_before  DECIMAL(18,4)   NOT NULL                 COMMENT '变动前余额',
    change_amount   DECIMAL(18,4)   NOT NULL                 COMMENT '变动金额（正=入账，负=出账）',
    balance_after   DECIMAL(18,4)   NOT NULL                 COMMENT '变动后余额',
    trade_type      TINYINT         NOT NULL                 COMMENT '交易类型：1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零,7-公共分摊',
    biz_no          VARCHAR(64)     NOT NULL                 COMMENT '关联业务单号',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作人',
    operate_time    DATETIME        NOT NULL                 COMMENT '操作时间',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注/原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_no (flow_no),
    KEY idx_customer_id (customer_id),
    KEY idx_biz_no (biz_no),
    KEY idx_trade_type (trade_type),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水/对账表';

-- 8.1 表计及虚拟表业务操作轨迹
CREATE TABLE IF NOT EXISTS t_meter_operation_trace (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT DEFAULT NULL,
    object_no VARCHAR(128) DEFAULT NULL,
    project_id BIGINT DEFAULT NULL,
    customer_id BIGINT DEFAULT NULL,
    customer_code_snapshot VARCHAR(64) DEFAULT NULL,
    customer_name_snapshot VARCHAR(128) DEFAULT NULL,
    meter_id BIGINT DEFAULT NULL,
    parent_meter_id BIGINT DEFAULT NULL,
    child_meter_id BIGINT DEFAULT NULL,
    event_type VARCHAR(32) NOT NULL,
    operation_terminal VARCHAR(16) DEFAULT 'API',
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(64) DEFAULT NULL,
    balance_before DECIMAL(18,4) DEFAULT NULL,
    change_amount DECIMAL(18,4) DEFAULT NULL,
    credited_amount DECIMAL(18,4) DEFAULT NULL,
    balance_after DECIMAL(18,4) DEFAULT NULL,
    previous_reading DECIMAL(18,4) DEFAULT NULL,
    raw_measurement DECIMAL(18,4) DEFAULT NULL,
    billed_measurement DECIMAL(18,4) DEFAULT NULL,
    consumption DECIMAL(18,4) DEFAULT NULL,
    ct_ratio DECIMAL(10,4) DEFAULT NULL,
    pt_ratio DECIMAL(10,4) DEFAULT NULL,
    unit_price DECIMAL(10,4) DEFAULT NULL,
    plus_price DECIMAL(10,4) DEFAULT NULL,
    rate_config_id BIGINT DEFAULT NULL,
    price_type TINYINT DEFAULT NULL,
    billing_type VARCHAR(32) DEFAULT NULL,
    payment_method VARCHAR(32) DEFAULT NULL,
    relation_before TEXT NULL,
    relation_after TEXT NULL,
    remark VARCHAR(512) DEFAULT NULL,
    request_id VARCHAR(128) DEFAULT NULL,
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    failure_reason VARCHAR(1000) DEFAULT NULL,
    detail_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_trace_object (object_type, object_id, operation_time),
    KEY idx_trace_event (event_type, operation_time),
    KEY idx_trace_customer (customer_id, operation_time),
    UNIQUE KEY uk_trace_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计及虚拟表业务操作轨迹';

-- 9. 每日冻结记录表
CREATE TABLE IF NOT EXISTS t_daily_freeze_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id     BIGINT          DEFAULT NULL             COMMENT '商户ID',
    record_date     DATE            NOT NULL                 COMMENT '冻结日期',
    kwh             DECIMAL(18,4)   DEFAULT NULL             COMMENT '组合有功总电量',
    kwhPeak         DECIMAL(18,4)   DEFAULT NULL             COMMENT '尖电量',
    kwhBusy         DECIMAL(18,4)   DEFAULT NULL             COMMENT '峰电量',
    kwhNormal       DECIMAL(18,4)   DEFAULT NULL             COMMENT '平电量',
    kwhIdle         DECIMAL(18,4)   DEFAULT NULL             COMMENT '谷电量',
    zhyg            DECIMAL(18,4)   DEFAULT NULL             COMMENT '组合有功总用电量',
    zxyg            DECIMAL(18,4)   DEFAULT NULL             COMMENT '正向有功总用电量',
    fxyg            DECIMAL(18,4)   DEFAULT NULL             COMMENT '反向有功总用电量',
    read_val        DECIMAL(18,4)   DEFAULT NULL             COMMENT '读数',
    freeze_time     DATETIME        DEFAULT NULL             COMMENT '冻结时间',
    data_source     VARCHAR(32)     DEFAULT NULL             COMMENT '数据来源',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_record_date (meter_id, record_date),
    KEY idx_record_date (record_date),
    KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日冻结记录表';

-- 10. 费率时段策略表（来自 migration_phase2_pricing）
CREATE TABLE IF NOT EXISTS t_price_strategy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    strategy_name       VARCHAR(128)    NOT NULL                 COMMENT '策略名称',
    strategy_code       VARCHAR(64)     NOT NULL                 COMMENT '策略编码（唯一）',
    version_group_code  VARCHAR(64)     DEFAULT NULL             COMMENT '费率版本组编码',
    version_no          INT             NOT NULL DEFAULT 1       COMMENT '版本号',
    project_id          BIGINT          DEFAULT NULL             COMMENT '关联项目ID',
    meter_type         TINYINT         NOT NULL DEFAULT 1       COMMENT '表计类型：0-水表，1-电表',
    price_type          INT             NOT NULL DEFAULT 2       COMMENT '策略类型：1-固定单价,2-分时电价,3-阶梯电价',
    time_slots          JSON            DEFAULT NULL             COMMENT '时段电价JSON',
    fixed_price         DECIMAL(10,4)   DEFAULT NULL             COMMENT '固定单价（price_type=1时使用）',
    plus_price          DECIMAL(10,4)   DEFAULT 0.0000           COMMENT '附加费（元/kWh）',
    effective_date      DATE            NOT NULL                 COMMENT '生效日期',
    expire_date         DATE            DEFAULT NULL             COMMENT '失效日期',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费率时段策略表';

-- 11. 费率配置表
CREATE TABLE IF NOT EXISTS t_rate_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          DEFAULT NULL             COMMENT '关联表计ID（NULL=全局默认配置）',
    meter_type_code VARCHAR(64)     DEFAULT NULL             COMMENT '表计类型编码',
    meter_type      VARCHAR(64)     DEFAULT NULL             COMMENT '表计类型',
    meter_conn_mode VARCHAR(32)     DEFAULT NULL             COMMENT '表计接线方式',
    wave_rate       VARCHAR(32)     DEFAULT NULL             COMMENT '波率',
    fact_code       VARCHAR(64)     DEFAULT NULL             COMMENT '厂家编码',
    price_type      INT             NOT NULL DEFAULT 1       COMMENT '电价类型',
    price_content   JSON            DEFAULT NULL             COMMENT '电价内容JSON',
    plus_price      DECIMAL(10,4)   DEFAULT 0.0000           COMMENT '附加费',
    effective_date  DATE            NOT NULL                 COMMENT '生效日期',
    expire_date     DATE            DEFAULT NULL             COMMENT '失效日期',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_meter_id (meter_id),
    KEY idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费率配置表';

-- 12. 充值订单表
CREATE TABLE IF NOT EXISTS t_recharge_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id     BIGINT          NOT NULL                 COMMENT '商户ID',
    order_no        VARCHAR(64)     NOT NULL                 COMMENT '订单编号',
    db_no           VARCHAR(64)     DEFAULT NULL             COMMENT '表号',
    amount          DECIMAL(18,4)   NOT NULL                 COMMENT '充值金额',
    payamount       DECIMAL(18,4)   DEFAULT NULL             COMMENT '支付金额',
    realamount      DECIMAL(18,4)   DEFAULT NULL             COMMENT '实际到账金额',
    balance         DECIMAL(18,4)   DEFAULT NULL             COMMENT '账户余额',
    charegetype     INT             DEFAULT NULL             COMMENT '充值类型',
    passrate        DECIMAL(10,4)   DEFAULT NULL             COMMENT '费率',
    srvfee          DECIMAL(10,4)   DEFAULT NULL             COMMENT '服务费',
    srvtpe          INT             DEFAULT NULL             COMMENT '服务类型',
    srvpaid         INT             DEFAULT NULL             COMMENT '服务费是否已付',
    czy             VARCHAR(64)     DEFAULT NULL             COMMENT '操作员',
    db_name         VARCHAR(128)    DEFAULT NULL             COMMENT '商户名',
    pro_name        VARCHAR(128)    DEFAULT NULL             COMMENT '项目名',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作员姓名',
    user_name       VARCHAR(64)     DEFAULT NULL             COMMENT '用户名',
    trade_type      INT             DEFAULT NULL             COMMENT '交易类型',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer_id (customer_id),
    KEY idx_meter_id (meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';

-- 13. 账单分类表
CREATE TABLE IF NOT EXISTS t_bill_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    category_name   VARCHAR(128)    NOT NULL                 COMMENT '分类名称',
    category_code   VARCHAR(64)     DEFAULT NULL             COMMENT '分类编码',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '分类描述',
    sort_order      INT             DEFAULT 0                COMMENT '排序号',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单分类表';

-- 14. 银行账户表
CREATE TABLE IF NOT EXISTS t_bank_account (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    bank_name       VARCHAR(128)    DEFAULT NULL             COMMENT '银行名称',
    account_name    VARCHAR(128)    DEFAULT NULL             COMMENT '账户名称',
    account_number  VARCHAR(64)     DEFAULT NULL             COMMENT '银行账号',
    bank_branch     VARCHAR(128)    DEFAULT NULL             COMMENT '开户支行',
    bank_code       VARCHAR(64)     DEFAULT NULL             COMMENT '开户行联行号',
    account_type    VARCHAR(32)     DEFAULT NULL             COMMENT '账户类型：COMPANY-对公,PERSONAL-个人',
    is_default      TINYINT         DEFAULT 0                COMMENT '是否默认：0-否,1-是',
    contact_person  VARCHAR(64)     DEFAULT NULL             COMMENT '联系人',
    contact_phone   VARCHAR(32)     DEFAULT NULL             COMMENT '联系电话',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户表';

-- 15. 账单记录表（含 recharge_request_id 字段，来自 migration_bill_add_recharge_link）
CREATE TABLE IF NOT EXISTS t_bill (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    bill_no             VARCHAR(64)     NOT NULL                 COMMENT '账单编号（全局唯一）',
    bill_name           VARCHAR(128)    NOT NULL                 COMMENT '账单名称',
    category_id         BIGINT          DEFAULT NULL             COMMENT '关联账单分类ID',
    category_name       VARCHAR(128)    DEFAULT NULL             COMMENT '账单类别名称',
    meter_id            BIGINT          DEFAULT NULL             COMMENT '关联表计ID',
    recharge_request_id BIGINT          DEFAULT NULL             COMMENT '关联小程序充值申请ID（FK→t_recharge_request.id）',
    meter_no            VARCHAR(64)     DEFAULT NULL             COMMENT '表号/钢印号',
    virtual_meter_id    BIGINT          DEFAULT NULL             COMMENT '关联虚拟表计ID',
    virtual_meter_no    VARCHAR(64)     DEFAULT NULL             COMMENT '虚拟表号',
    merchant_name       VARCHAR(128)    DEFAULT NULL             COMMENT '商户名',
    virtual_merchant_name VARCHAR(128)  DEFAULT NULL             COMMENT '虚拟表商户名',
    received_amount     DECIMAL(18,4)   NOT NULL DEFAULT 0.0000  COMMENT '收款金额（元）',
    pending_amount      DECIMAL(18,4)   NOT NULL DEFAULT 0.0000  COMMENT '待收金额（元）',
    payment_status      VARCHAR(32)     NOT NULL DEFAULT '未支付' COMMENT '支付状态：已支付/未支付/待审核',
    arrears_handling    VARCHAR(32)     DEFAULT NULL             COMMENT '欠费处理方式',
    support_online_payment TINYINT      NOT NULL DEFAULT 1       COMMENT '是否支持线上支付',
    description         VARCHAR(512)    DEFAULT NULL             COMMENT '账单描述',
    bill_create_time    DATETIME        DEFAULT NULL             COMMENT '账单生成时间',
    last_payment_time   DATETIME        DEFAULT NULL             COMMENT '最后支付时间',
    actual_payment_time DATETIME        DEFAULT NULL             COMMENT '实际支付时间',
    remark              VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no),
    KEY idx_payment_status (payment_status),
    KEY idx_merchant_name (merchant_name),
    KEY idx_meter_no (meter_no),
    KEY idx_bill_create_time (bill_create_time),
    KEY idx_category_id (category_id),
    KEY idx_virtual_meter_no (virtual_meter_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单记录表（即时账单持久化）';


-- ============================================================
-- 三、报表预聚合表
-- ============================================================

-- 16. 表计日报表（水+电统一，凌晨预聚合，来自 schema.sql + migration_phase6）
CREATE TABLE IF NOT EXISTS tb_meter_daily_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          NOT NULL                 COMMENT '项目ID',
    group_id        BIGINT          DEFAULT NULL             COMMENT '分组ID',
    merchant_id     BIGINT          DEFAULT NULL             COMMENT '商户ID',
    device_no       VARCHAR(64)     NOT NULL                 COMMENT '设备号',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计日报表（水+电统一）';

-- 17. 表计月报表（含正向/反向有功、余额快照等 Phase 7 字段）
CREATE TABLE IF NOT EXISTS tb_meter_monthly_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          NOT NULL                 COMMENT '项目ID',
    group_id        BIGINT          DEFAULT NULL             COMMENT '分组ID',
    merchant_id     BIGINT          DEFAULT NULL             COMMENT '商户ID',
    device_no       VARCHAR(64)     NOT NULL                 COMMENT '设备号',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    meter_type      VARCHAR(16)     NOT NULL DEFAULT 'ELECTRIC' COMMENT '表计类型',
    total_usage     DECIMAL(14,4)   NOT NULL DEFAULT 0       COMMENT '月度总用量',
    peak_usage      DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT '尖时段月度用量',
    busy_usage      DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT '峰时段月度用量',
    normal_usage    DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT '平时段月度用量',
    idle_usage      DECIMAL(18,4)   NOT NULL DEFAULT 0       COMMENT '谷时段月度用量',
    -- Phase 7 正向/反向有功字段
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
    stat_month      VARCHAR(7)      NOT NULL                 COMMENT '统计月份（YYYY-MM）',
    stat_year       VARCHAR(4)      NOT NULL                 COMMENT '统计年份',
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

-- 18. 用电量日报表（专用于电费日报统计，来自 migration_phase3_report，含后续扩展字段）
CREATE TABLE IF NOT EXISTS t_report_daily_electricity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    report_date         DATE            NOT NULL                 COMMENT '报表日期',
    meter_id            BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id         BIGINT          DEFAULT NULL             COMMENT '商户ID',
    group_id            BIGINT          DEFAULT NULL             COMMENT '分组ID',
    electricity_consumed DECIMAL(18,4)  NOT NULL DEFAULT 0.0000  COMMENT '当日用电量（kWh）',
    -- 正向/反向有功（来自 migration_add_forward_reverse_power）
    forward_consumed    DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '正向有功日用电量（kWh）',
    reverse_consumed    DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '反向有功日用电量（kWh）',
    -- 分时用电量
    peak_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '尖时段用电量（kWh）',
    busy_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '峰时段用电量（kWh）',
    normal_consumed     DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '平时段用电量（kWh）',
    idle_consumed       DECIMAL(18,4)   DEFAULT 0.0000           COMMENT '谷时段用电量（kWh）',
    -- 分时扣费金额（来自 migration_add_time_of_use_charges）
    peak_charge_amount   DECIMAL(14,4)  DEFAULT 0.0000           COMMENT '尖时段扣费金额（元）',
    busy_charge_amount   DECIMAL(14,4)  DEFAULT 0.0000           COMMENT '峰时段扣费金额（元）',
    normal_charge_amount DECIMAL(14,4)  DEFAULT 0.0000           COMMENT '平时段扣费金额（元）',
    idle_charge_amount   DECIMAL(14,4)  DEFAULT 0.0000           COMMENT '谷时段扣费金额（元）',
    -- 总扣费
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


-- ============================================================
-- 四、业务扩展表（分组 / 退费 / 水表）
-- ============================================================

-- 19. 分组管理表（来自 migration_phase4_infra）
CREATE TABLE IF NOT EXISTS t_meter_group (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    parent_id       BIGINT          DEFAULT NULL             COMMENT '父分组ID，NULL表示顶级分组',
    group_name      VARCHAR(128)    NOT NULL                 COMMENT '分组名称',
    manage_type     VARCHAR(16)     NOT NULL DEFAULT 'METERING' COMMENT '分组管理类型：BILLING计费管理，METERING计量管理',
    dimension_type  VARCHAR(16)     NOT NULL DEFAULT 'NORMAL' COMMENT '统计维度：NORMAL/PROJECT/AREA/CATEGORY',
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
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组管理表（支持层级结构）';

-- 20. 人工退费日志表（来自 migration_phase2_pricing）
CREATE TABLE IF NOT EXISTS t_refund_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_no             VARCHAR(64)     NOT NULL                 COMMENT '流水号',
    meter_id            BIGINT          NOT NULL                 COMMENT '关联表计ID',
    customer_id         BIGINT          NOT NULL                 COMMENT '关联商户ID',
    balance_before      DECIMAL(18,4)   NOT NULL                 COMMENT '退费前余额',
    refund_amount       DECIMAL(18,4)   NOT NULL                 COMMENT '退费金额（元，正数）',
    balance_after       DECIMAL(18,4)   NOT NULL                 COMMENT '退费后余额',
    operator_id         BIGINT          NOT NULL                 COMMENT '操作员ID',
    operator_name       VARCHAR(64)     DEFAULT NULL             COMMENT '操作员姓名',
    refund_reason       VARCHAR(512)    NOT NULL                 COMMENT '退费原因',
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

-- 20a. 区域表
CREATE TABLE IF NOT EXISTS t_area (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    area_code       VARCHAR(64)     NOT NULL                 COMMENT '区域编码',
    area_name       VARCHAR(128)    NOT NULL                 COMMENT '区域名称',
    parent_code     VARCHAR(64)     DEFAULT NULL             COMMENT '父级区域编码',
    memo            VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_area_code (area_code),
    KEY idx_parent_code (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

-- 20b. 余额调账单表
CREATE TABLE IF NOT EXISTS t_balance_adjustment (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    adjustment_no   VARCHAR(64)     NOT NULL                 COMMENT '调账单编号',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id     BIGINT          NOT NULL                 COMMENT '商户ID',
    db_no           VARCHAR(64)     DEFAULT NULL             COMMENT '表号',
    balance         DECIMAL(18,4)   NOT NULL                 COMMENT '调账金额',
    adjustment_type VARCHAR(32)     DEFAULT NULL             COMMENT '调账类型',
    original_price  DECIMAL(10,4)   DEFAULT NULL             COMMENT '原电价',
    original_surcharge DECIMAL(10,4) DEFAULT NULL            COMMENT '原附加费',
    adjust_price    DECIMAL(10,4)   DEFAULT NULL             COMMENT '调整后电价',
    adjust_surcharge DECIMAL(10,4)  DEFAULT NULL             COMMENT '调整后附加费',
    payment_status  VARCHAR(32)     DEFAULT NULL             COMMENT '支付状态',
    deduct_method   VARCHAR(32)     DEFAULT NULL             COMMENT '扣款方式',
    deduct_time     DATETIME        DEFAULT NULL             COMMENT '扣款时间',
    group_name      VARCHAR(128)    DEFAULT NULL             COMMENT '分组名称',
    bz              VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作员姓名',
    adjustment_time DATETIME        DEFAULT NULL             COMMENT '调账时间',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_adjustment_no (adjustment_no),
    KEY idx_meter_id (meter_id),
    KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额调账单表';

-- 20c. 表计状态表
CREATE TABLE IF NOT EXISTS t_meter_state (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_code   VARCHAR(64)     DEFAULT NULL             COMMENT '商户编码',
    steal_no        VARCHAR(64)     DEFAULT NULL             COMMENT '表钢印号',
    vender          VARCHAR(64)     DEFAULT NULL             COMMENT '厂商代码',
    status          VARCHAR(32)     DEFAULT NULL             COMMENT '欠费状态',
    message         VARCHAR(512)    DEFAULT NULL             COMMENT '状态消息',
    record_date     DATETIME        DEFAULT NULL             COMMENT '记录时间',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_meter_id (meter_id),
    KEY idx_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计状态表';

-- 20d. 表计类型表
CREATE TABLE IF NOT EXISTS t_meter_type (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_type_code VARCHAR(64)     DEFAULT NULL             COMMENT '表计类型编码',
    meter_type      VARCHAR(64)     DEFAULT NULL             COMMENT '表计类型',
    meter_conn_mode VARCHAR(32)     DEFAULT NULL             COMMENT '接线方式',
    wave_rate       VARCHAR(32)     DEFAULT NULL             COMMENT '波率',
    caliber         VARCHAR(16)     DEFAULT NULL             COMMENT '口径',
    minuent         VARCHAR(32)     DEFAULT NULL             COMMENT '最小单位',
    fact_code       VARCHAR(64)     DEFAULT NULL             COMMENT '厂家编码',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_type_code (meter_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表计类型表';

-- 20e. 通知公告表
CREATE TABLE IF NOT EXISTS t_notification (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(256)    NOT NULL                 COMMENT '标题',
    content         TEXT            DEFAULT NULL             COMMENT '内容',
    publish_time    DATETIME        DEFAULT NULL             COMMENT '发布时间',
    publisher       VARCHAR(64)     DEFAULT NULL             COMMENT '发布人',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 20f. 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作人',
    module_name     VARCHAR(128)    DEFAULT NULL             COMMENT '操作模块',
    operation_type  VARCHAR(32)     DEFAULT NULL             COMMENT '操作类型',
    operation_desc  VARCHAR(512)    DEFAULT NULL             COMMENT '操作描述',
    request_params  TEXT            DEFAULT NULL             COMMENT '请求参数',
    response_result TEXT            DEFAULT NULL             COMMENT '响应结果',
    ip_address      VARCHAR(64)     DEFAULT NULL             COMMENT 'IP地址',
    operation_time  DATETIME        DEFAULT NULL             COMMENT '操作时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operator_name (operator_name),
    KEY idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 20g. 用电分析表
CREATE TABLE IF NOT EXISTS t_power_analysis (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id            BIGINT          NOT NULL                 COMMENT '表计ID',
    db_no               VARCHAR(64)     DEFAULT NULL             COMMENT '表号',
    balance             DOUBLE          DEFAULT NULL             COMMENT '余额',
    avg_cost_money      DOUBLE          DEFAULT NULL             COMMENT '平均扣费金额',
    to_month_electricity DOUBLE         DEFAULT NULL             COMMENT '本月累计用电量',
    to_day_electricity  DOUBLE          DEFAULT NULL             COMMENT '本日累计用电量',
    sync_time           DATETIME        DEFAULT NULL             COMMENT '同步时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_id (meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用电分析表';

-- 20h. 公共分摊表
CREATE TABLE IF NOT EXISTS t_public_allocation (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          DEFAULT NULL             COMMENT '项目ID',
    source_meter_id BIGINT          NOT NULL                 COMMENT '源表计ID',
    allocation_name VARCHAR(128)    DEFAULT NULL             COMMENT '分摊名称',
    allocation_type VARCHAR(32)     DEFAULT NULL             COMMENT '分摊类型',
    total_amount    DECIMAL(18,4)   NOT NULL                 COMMENT '分摊总额',
    allocation_detail JSON          DEFAULT NULL             COMMENT '分摊明细JSON',
    effective_date  DATE            NOT NULL                 COMMENT '生效日期',
    expire_date     DATE            DEFAULT NULL             COMMENT '失效日期',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project_id (project_id),
    KEY idx_source_meter_id (source_meter_id),
    KEY idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共分摊表';

-- 20i. 退费明细表
CREATE TABLE IF NOT EXISTS t_refund_detail (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    refund_no       VARCHAR(64)     NOT NULL                 COMMENT '退费编号',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    customer_id     BIGINT          NOT NULL                 COMMENT '商户ID',
    refund_amount   DECIMAL(18,4)   NOT NULL                 COMMENT '退费金额',
    refund_type     VARCHAR(32)     DEFAULT NULL             COMMENT '退费类型',
    refund_reason   VARCHAR(512)    DEFAULT NULL             COMMENT '退费原因',
    original_order_id BIGINT        DEFAULT NULL             COMMENT '原充值订单ID',
    operator_name   VARCHAR(64)     DEFAULT NULL             COMMENT '操作员姓名',
    refund_time     DATETIME        DEFAULT NULL             COMMENT '退费时间',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_meter_id (meter_id),
    KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退费明细表';

-- 20j. 虚拟表（影子表计）表
CREATE TABLE IF NOT EXISTS t_shadow_meter (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    project_id      BIGINT          DEFAULT NULL             COMMENT '项目ID',
    parent_meter_id BIGINT          DEFAULT NULL             COMMENT '父表计ID（商户型虚拟表为空）',
    child_meter_id  BIGINT          NOT NULL                 COMMENT '子表计ID',
    shadow_type     VARCHAR(32)     DEFAULT NULL             COMMENT '虚拟表类型',
    coefficient     DECIMAL(10,4)   DEFAULT 1.0000           COMMENT '分摊系数',
    relation_desc   VARCHAR(256)    DEFAULT NULL             COMMENT '关系描述',
    effective_date  DATE            NOT NULL                 COMMENT '生效日期',
    expire_date     DATE            DEFAULT NULL             COMMENT '失效日期',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_child (parent_meter_id, child_meter_id),
    KEY idx_project_id (project_id),
    KEY idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟表（影子表计）表';

-- 20k. 阀门控制记录表
CREATE TABLE IF NOT EXISTS t_valve_control (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT          NOT NULL                 COMMENT '表计ID',
    meteraddress    VARCHAR(64)     DEFAULT NULL             COMMENT '表计通讯地址',
    companycode     VARCHAR(64)     DEFAULT NULL             COMMENT '厂家编号',
    customerno      VARCHAR(64)     DEFAULT NULL             COMMENT '用户编号',
    sendbody        TEXT            DEFAULT NULL             COMMENT '发送指令体',
    ctrl_code1      INT             DEFAULT NULL             COMMENT '控制码1',
    ctrl_code2      INT             DEFAULT NULL             COMMENT '控制码2',
    batchid         VARCHAR(64)     DEFAULT NULL             COMMENT '批次ID',
    task_status     VARCHAR(32)     DEFAULT NULL             COMMENT '任务状态',
    type            VARCHAR(16)     DEFAULT NULL             COMMENT '类型',
    steal_no        VARCHAR(64)     DEFAULT NULL             COMMENT '表钢印号',
    vender          VARCHAR(64)     DEFAULT NULL             COMMENT '厂商代码',
    operator        VARCHAR(64)     DEFAULT NULL             COMMENT '操作人',
    sign            VARCHAR(64)     DEFAULT NULL             COMMENT '签名',
    operate_date    DATETIME        DEFAULT NULL             COMMENT '操作时间',
    returnvalue     VARCHAR(512)    DEFAULT NULL             COMMENT '返回值',
    msg             VARCHAR(512)    DEFAULT NULL             COMMENT '消息',
    state_valve     INT             DEFAULT NULL             COMMENT '阀门状态',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       VARCHAR(64)     DEFAULT NULL             COMMENT '创建人',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_meter_id (meter_id),
    KEY idx_operate_date (operate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阀门控制记录表';

-- 20l. 厂商表
CREATE TABLE IF NOT EXISTS t_vender (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    fact_code       VARCHAR(64)     NOT NULL                 COMMENT '厂商编码',
    fact_name       VARCHAR(128)    NOT NULL                 COMMENT '厂商名称',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fact_code (fact_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商表';

-- 20m. 电表厂商对接配置表
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

-- 20n. 水表厂商对接配置表
CREATE TABLE IF NOT EXISTS t_water_meter_vendor (
    id                 BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    vendor_name        VARCHAR(128)  NOT NULL                 COMMENT '水表厂商名称',
    vendor_code        VARCHAR(64)   NOT NULL                 COMMENT '厂商编码',
    protocol_type      VARCHAR(64)   NOT NULL                 COMMENT '协议类型',
    api_url            VARCHAR(512)  DEFAULT NULL             COMMENT 'API基础地址',
    auth_type          VARCHAR(64)   DEFAULT NULL             COMMENT '认证方式',
    app_id             VARCHAR(128)  DEFAULT NULL             COMMENT 'AppId',
    app_secret         VARCHAR(256)  DEFAULT NULL             COMMENT 'AppSecret',
    webhook_signature  VARCHAR(256)  DEFAULT NULL             COMMENT 'Webhook签名密钥',
    config_json        JSON          DEFAULT NULL             COMMENT '厂商扩展配置JSON',
    status             TINYINT       NOT NULL DEFAULT 1       COMMENT '状态：0-停用，1-启用',
    last_enabled_time  DATETIME      DEFAULT NULL             COMMENT '最近一次启用时间',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          VARCHAR(64)   DEFAULT NULL             COMMENT '创建人',
    is_deleted         TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_vendor_code (vendor_code),
    KEY idx_protocol_type (protocol_type),
    KEY idx_status (status),
    KEY idx_last_enabled_time (last_enabled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水表厂商对接配置表';

-- 21. 水表主表（来自 migration_water_meter_test_data）
CREATE TABLE IF NOT EXISTS t_water_meter (
    meter_id       BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '水表主键ID',
    merchant_id    BIGINT       DEFAULT NULL             COMMENT '关联商户ID',
    device_no      VARCHAR(64)  NOT NULL                 COMMENT '设备编号',
    meter_name     VARCHAR(128) DEFAULT NULL             COMMENT '水表名称',
    caliber        VARCHAR(16)  DEFAULT NULL             COMMENT '口径',
    status         INT          DEFAULT 1                COMMENT '运行状态',
    online         INT          DEFAULT 1                COMMENT '在线状态',
    install_time   DATETIME     DEFAULT NULL             COMMENT '安装时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted     TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (meter_id),
    UNIQUE KEY uk_device_no (device_no),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水表主表';

-- 22. 水表读数流水表（来自 migration_water_meter_test_data）
CREATE TABLE IF NOT EXISTS t_water_meter_reading (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    meter_id        BIGINT       NOT NULL                 COMMENT '关联水表ID',
    record_time     DATETIME     NOT NULL                 COMMENT '读数时间戳',
    usage_amount    DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '用水量(吨)',
    fee             DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '费用(元)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_meter_id (meter_id),
    KEY idx_record_time (record_time),
    KEY idx_meter_time (meter_id, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水表读数流水表';


-- ============================================================
-- 五、小程序相关表（来自 migration_miniapp）
-- ============================================================

-- 23. 小程序用户表
CREATE TABLE IF NOT EXISTS t_miniapp_user (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    openid           VARCHAR(128)    DEFAULT NULL             COMMENT '微信OpenID',
    unionid          VARCHAR(128)    DEFAULT NULL             COMMENT '微信UnionID',
    phone            VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    customer_id      BIGINT          DEFAULT NULL             COMMENT '绑定的商户ID，首次登录待授权时可为空',
    nickname         VARCHAR(64)     DEFAULT NULL             COMMENT '用户昵称',
    avatar_url       VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL',
    login_type       TINYINT         NOT NULL DEFAULT 0       COMMENT '登录方式: 0=微信登录, 1=手机号登录',
    status           TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 0=禁用, 1=正常',
    last_login_time  DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    token_version    INT             NOT NULL DEFAULT 1       COMMENT 'JWT令牌版本',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted       TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    UNIQUE KEY uk_phone (phone),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户表';

-- 24. 小程序充值申请表
CREATE TABLE IF NOT EXISTS t_recharge_request (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    request_no        VARCHAR(32)     NOT NULL                 COMMENT '申请编号',
    user_id           BIGINT          NOT NULL                 COMMENT '小程序用户ID',
    customer_id       BIGINT          NOT NULL                 COMMENT '商户ID',
    meter_id          BIGINT          NOT NULL                 COMMENT '表计ID',
    amount            DECIMAL(18,4)   NOT NULL                 COMMENT '充值金额',
    pay_method        VARCHAR(32)     NOT NULL DEFAULT 'VIRTUAL' COMMENT '支付方式',
    audit_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
    audit_remark      VARCHAR(255)    DEFAULT NULL             COMMENT '审核备注',
    audit_user_id     BIGINT          DEFAULT NULL             COMMENT '审核人ID',
    audit_time        DATETIME        DEFAULT NULL             COMMENT '审核时间',
    related_order_no  VARCHAR(64)     DEFAULT NULL             COMMENT '关联充值订单号',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_no (request_no),
    INDEX idx_user_id (user_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_meter_id (meter_id),
    INDEX idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序充值申请表';

-- 25. 票据表
CREATE TABLE IF NOT EXISTS t_invoice (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    invoice_no        VARCHAR(32)     NOT NULL                 COMMENT '票据编号',
    customer_id       BIGINT          NOT NULL                 COMMENT '商户ID',
    meter_id          BIGINT          DEFAULT NULL             COMMENT '表计ID',
    recharge_order_id BIGINT          DEFAULT NULL             COMMENT '关联充值订单ID',
    amount            DECIMAL(18,4)   NOT NULL                 COMMENT '票据金额',
    invoice_type      VARCHAR(32)     NOT NULL DEFAULT 'ELECTRIC' COMMENT '票据类型: ELECTRIC/WATER',
    invoice_status    VARCHAR(32)     NOT NULL DEFAULT 'NOT_ISSUED' COMMENT '开票状态: NOT_ISSUED/ISSUED',
    issue_time        DATETIME        DEFAULT NULL             COMMENT '开票时间',
    remark            VARCHAR(255)    DEFAULT NULL             COMMENT '备注',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (invoice_no),
    INDEX idx_customer_id (customer_id),
    INDEX idx_meter_id (meter_id),
    INDEX idx_invoice_status (invoice_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据表';


-- ============================================================
-- 六、默认种子数据（角色 + 用户 + 费率 + 定价策略）
-- ============================================================

-- 默认角色
INSERT IGNORE INTO t_role (role_name, role_code, permissions, status) VALUES
('系统管理员', 'ADMIN',    '["meter:view","meter:edit","meter:delete","customer:view","customer:edit","customer:delete","report:view","report:export","user:manage","role:manage","system:config"]', 1),
('运营人员',   'OPERATOR', '["meter:view","meter:edit","customer:view","customer:edit","report:view","report:export"]', 1),
('审计员',     'AUDITOR',  '["meter:view","customer:view","report:view","report:export","log:view"]', 1);

-- ═══════════════════════════════════════════════════════════
-- 默认管理员和运营人员
-- ═══════════════════════════════════════════════════════════
-- 注意：管理员的初始密码不再写入 SQL 文件中。
-- 部署前请设置环境变量：
--   ADMIN_INITIAL_PASSWORD=<您的管理员密码>
--   OPERATOR_INITIAL_PASSWORD=<您的运营人员密码>
-- 应用首次启动时，DataInitializer 会读取环境变量、BCrypt 加密后写入数据库。
-- 若环境变量未配置，生产环境下不会创建账号（安全默认行为）。
-- 若需通过 SQL 直接部署用户，请使用 GenerateBcryptHash 工具生成 BCrypt 哈希值后手动 INSERT。

-- 默认全局费率策略（固定电价 0.85 元/kWh，兜底用）
INSERT IGNORE INTO t_price_strategy (strategy_name, strategy_code, price_type, fixed_price, plus_price, effective_date, status, remark)
VALUES ('默认固定电价', 'DEFAULT_FIXED', 1, 0.8500, 0.0000, CURDATE(), 1, '系统默认全局固定电价策略');

-- 默认全局费率配置（固定单价 0.85 元/kWh）
INSERT IGNORE INTO t_rate_config (meter_id, price_type, price_content, plus_price, effective_date, status, create_time, update_time, is_deleted)
VALUES (NULL, 1, '{"price":0.85}', 0.0000, '2026-01-01', 1, NOW(), NOW(), 0);


-- ============================================================
-- 部署验证查询
-- ============================================================
SELECT '========================================' AS '';
SELECT '  部署验证结果' AS '';
SELECT '========================================' AS '';
SELECT TABLE_NAME AS 表名, TABLE_COMMENT AS 说明
FROM information_schema.TABLES
WHERE table_schema = 'energy_platform'
ORDER BY TABLE_NAME;

SELECT COUNT(*) AS 总表数
FROM information_schema.TABLES
WHERE table_schema = 'energy_platform';

SELECT role_code AS 角色编码, role_name AS 角色名称 FROM t_role;

SELECT '' AS '';
SELECT '部署完成。请通过环境变量 ADMIN_INITIAL_PASSWORD 和 OPERATOR_INITIAL_PASSWORD 配置初始密码后启动应用。' AS 提示;
