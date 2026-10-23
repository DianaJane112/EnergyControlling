-- =====================================================

CREATE TABLE IF NOT EXISTS t_miniapp_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(128), unionid VARCHAR(128), phone VARCHAR(32), customer_id BIGINT,
    nickname VARCHAR(128), avatar_url VARCHAR(512), login_type INT, status INT DEFAULT 1,
    last_login_time TIMESTAMP, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, token_version INT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_virtual_meter_viewer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    virtual_meter_id BIGINT NOT NULL,
    miniapp_user_id BIGINT NOT NULL,
    permission_type VARCHAR(16) NOT NULL DEFAULT 'VIEW',
    status TINYINT NOT NULL DEFAULT 1,
    effective_time TIMESTAMP, expire_time TIMESTAMP, remark VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64), is_deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_virtual_meter_viewer UNIQUE (virtual_meter_id, miniapp_user_id)
);
-- Energy Controller Platform — H2 Test Schema
-- 仅包含单元测试必要的核心表（H2 MySQL兼容模式）
-- =====================================================

-- 1. 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id              BIGINT AUTO_INCREMENT,
    role_name       VARCHAR(64)     NOT NULL,
    role_code       VARCHAR(64)     NOT NULL,
    permissions     VARCHAR(4096),
    status          TINYINT         NOT NULL DEFAULT 1,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (role_code)
);

-- 2. 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT AUTO_INCREMENT,
    username        VARCHAR(64)     NOT NULL,
    password        VARCHAR(256)    NOT NULL,
    real_name       VARCHAR(64),
    phone           VARCHAR(32),
    role            VARCHAR(32)     NOT NULL DEFAULT 'OPERATOR',
    status          TINYINT         NOT NULL DEFAULT 1,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_username UNIQUE (username)
);

-- 2. 商户档案表
CREATE TABLE IF NOT EXISTS t_customer (
    id              BIGINT AUTO_INCREMENT,
    customer_code   VARCHAR(64)     NOT NULL,
    customer_name   VARCHAR(128)    NOT NULL,
    area_name       VARCHAR(128),
    address         VARCHAR(256),
    phone           VARCHAR(32),
    customer_type   VARCHAR(32),
    use_nature      VARCHAR(32),
    create_date     TIMESTAMP,
    type            VARCHAR(4),
    old_steal_no    VARCHAR(64),
    old_val         VARCHAR(32),
    balance         DECIMAL(18,4)   NOT NULL DEFAULT 0.0000,
    status          TINYINT         NOT NULL DEFAULT 1,
    operator        VARCHAR(64),
    sign            VARCHAR(64),
    remark          VARCHAR(512),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(64),
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_code UNIQUE (customer_code)
);

-- 3. 表计主表
CREATE TABLE IF NOT EXISTS t_meter (
    id              BIGINT AUTO_INCREMENT,
    customer_id     BIGINT,
    meter_kind      VARCHAR(16)     DEFAULT 'PHYSICAL' NOT NULL,
    group_id        BIGINT,
    steal_no        VARCHAR(64),
    vender          VARCHAR(64),
    meter_type_name VARCHAR(64),
    caliber         VARCHAR(16),
    switch_op       VARCHAR(4),
    start_val       VARCHAR(32),
    terminal_name   VARCHAR(128),
    channel_type    VARCHAR(8),
    meter_phone     VARCHAR(32),
    telecomm_sn     VARCHAR(32),
    imei            VARCHAR(32),
    iccid           VARCHAR(32),
    address         VARCHAR(256),
    create_date     TIMESTAMP,
    type            VARCHAR(4),
    sign            VARCHAR(64),
    db_no           VARCHAR(12),
    name            VARCHAR(128),
    bei             INT             DEFAULT 1,
    bjlx            INT,
    meter_type      INT,
    credit          DECIMAL(18,4)   DEFAULT 0.0000,
    warnline        DECIMAL(18,4)   DEFAULT 0.0000,
    creatuser       VARCHAR(64),
    cttime          VARCHAR(32),
    feetype         INT             DEFAULT 1,
    pro_id          BIGINT,
    pro_name        VARCHAR(128),
    plus_price      DECIMAL(10,4)   DEFAULT 0.0000,
    price_type      INT             DEFAULT 1,
    price_content   VARCHAR(4096),
    join_phone      VARCHAR(4096),
    collector_device_no VARCHAR(64),
    phase           INT,
    meteraddress    VARCHAR(64),
    companycode     VARCHAR(64),
    water_vendor_id BIGINT,
    customerno      VARCHAR(64),
    meter_caliber   VARCHAR(16),
    controlling     INT,
    state_valve     INT,
    signal_strength INT,
    battery_voltage DECIMAL(10,4),
    status          INT             DEFAULT 1,
    online          INT             DEFAULT 1,
    meter_signal    INT,
    last_settled_reading DECIMAL(18,4),
    last_settled_time    TIMESTAMP,
    rate_config_id  BIGINT,
    ct_ratio        DECIMAL(10,4),
    pt_ratio        DECIMAL(10,4),
    mark_status     VARCHAR(16)     DEFAULT 'NORMAL',
    enabled_time    TIMESTAMP,
    disable_warning TINYINT         DEFAULT 0,
    comm_status_desc VARCHAR(64),
    collectorCode   VARCHAR(64),
    meterCode       BIGINT,
    bpsType         VARCHAR(16)     DEFAULT 'B2400',
    bitType         VARCHAR(8)      DEFAULT 'EVEN',
    protoType       VARCHAR(32)     DEFAULT 'DLT645_2007',
    serial          INT,
    deviceType      VARCHAR(16),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(64),
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_meter_operation_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT,
    object_no VARCHAR(128),
    project_id BIGINT,
    customer_id BIGINT,
    customer_code_snapshot VARCHAR(64),
    customer_name_snapshot VARCHAR(128),
    meter_id BIGINT,
    parent_meter_id BIGINT,
    child_meter_id BIGINT,
    event_type VARCHAR(32) NOT NULL,
    operation_terminal VARCHAR(16) DEFAULT 'API',
    operator_id BIGINT,
    operator_name VARCHAR(64),
    balance_before DECIMAL(18,4),
    change_amount DECIMAL(18,4),
    credited_amount DECIMAL(18,4),
    balance_after DECIMAL(18,4),
    previous_reading DECIMAL(18,4),
    raw_measurement DECIMAL(18,4),
    billed_measurement DECIMAL(18,4),
    consumption DECIMAL(18,4),
    ct_ratio DECIMAL(10,4),
    pt_ratio DECIMAL(10,4),
    unit_price DECIMAL(10,4),
    plus_price DECIMAL(10,4),
    rate_config_id BIGINT,
    price_type TINYINT,
    billing_type VARCHAR(32),
    payment_method VARCHAR(32),
    relation_before CLOB,
    relation_after CLOB,
    remark VARCHAR(512),
    request_id VARCHAR(128),
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    result_status VARCHAR(16) DEFAULT 'SUCCESS' NOT NULL,
    failure_reason VARCHAR(1000),
    detail_json VARCHAR(4096),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted TINYINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_trace_request UNIQUE (request_id)
);

-- 4. 资金流水表
CREATE TABLE IF NOT EXISTS t_account_flow (
    id              BIGINT AUTO_INCREMENT,
    flow_no         VARCHAR(64)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    meter_id        BIGINT,
    balance_before  DECIMAL(18,4)   NOT NULL,
    change_amount   DECIMAL(18,4)   NOT NULL,
    balance_after   DECIMAL(18,4)   NOT NULL,
    trade_type      TINYINT         NOT NULL,
    biz_no          VARCHAR(64)     NOT NULL,
    operator_name   VARCHAR(64),
    operate_time    TIMESTAMP       NOT NULL,
    remark          VARCHAR(512),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_flow_no UNIQUE (flow_no)
);

-- 5. 充值订单表
CREATE TABLE IF NOT EXISTS t_recharge_order (
    id              BIGINT AUTO_INCREMENT,
    order_no        VARCHAR(64)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    meter_id        BIGINT,
    amount          DECIMAL(18,4)   NOT NULL,
    payamount       DECIMAL(18,4),
    realamount      DECIMAL(18,4),
    balance         DECIMAL(18,4),
    charegetype     INT             DEFAULT 2,
    czy             VARCHAR(64),
    operator_name   VARCHAR(64),
    user_name       VARCHAR(64),
    trade_type      INT,
    remark          VARCHAR(512),
    order_status    VARCHAR(32)     DEFAULT 'SUCCESS',
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

-- 6. 退费明细表
CREATE TABLE IF NOT EXISTS t_refund_detail (
    id              BIGINT AUTO_INCREMENT,
    refund_no       VARCHAR(64)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    refund_amount   DECIMAL(18,4)   NOT NULL,
    refund_type     VARCHAR(32),
    refund_reason   VARCHAR(512),
    operator_name   VARCHAR(64),
    refund_time     TIMESTAMP,
    remark          VARCHAR(512),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_refund_no UNIQUE (refund_no)
);

-- 7. 余额调整表
CREATE TABLE IF NOT EXISTS t_balance_adjustment (
    id              BIGINT AUTO_INCREMENT,
    adjustment_no   VARCHAR(64)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    balance         DECIMAL(18,4)   NOT NULL,
    adjustment_type VARCHAR(32),
    bz              VARCHAR(512),
    operator_name   VARCHAR(64),
    adjustment_time TIMESTAMP,
    remark          VARCHAR(512),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_adjustment_no UNIQUE (adjustment_no)
);

-- 8. 票据表（供小程序端查询）
CREATE TABLE IF NOT EXISTS t_invoice (
    id               BIGINT AUTO_INCREMENT,
    invoice_no       VARCHAR(64)     NOT NULL,
    customer_id      BIGINT          NOT NULL,
    meter_id         BIGINT,
    recharge_order_id BIGINT,
    amount           DECIMAL(18,4)   NOT NULL,
    invoice_type     VARCHAR(32),
    invoice_status   VARCHAR(32)     DEFAULT 'NOT_ISSUED',
    remark           VARCHAR(512),
    create_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_no UNIQUE (invoice_no)
);

-- 9. 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id              BIGINT AUTO_INCREMENT,
    operator_name   VARCHAR(64),
    module          VARCHAR(128),
    business_type   VARCHAR(32),
    description     VARCHAR(1024),
    request_params  VARCHAR(4096),
    ip_address      VARCHAR(64),
    status          VARCHAR(16)     DEFAULT 'SUCCESS',
    error_msg       VARCHAR(1024),
    operate_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 10. 充值申请表（小程序端）
CREATE TABLE IF NOT EXISTS t_recharge_request (
    id              BIGINT AUTO_INCREMENT,
    request_no      VARCHAR(64)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    customer_id     BIGINT          NOT NULL,
    amount          DECIMAL(18,4)   NOT NULL,
    audit_status    VARCHAR(32)     DEFAULT 'PENDING',
    audit_comment   VARCHAR(512),
    auditor_id      BIGINT,
    audit_time      TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_request_no UNIQUE (request_no)
);
CREATE TABLE IF NOT EXISTS t_billing_segment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(64) NOT NULL,
    virtual_meter_id BIGINT NOT NULL,
    source_meter_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    billing_date DATE NOT NULL,
    period_start TIMESTAMP,
    period_end TIMESTAMP NOT NULL,
    previous_reading DECIMAL(20,6),
    current_reading DECIMAL(20,6),
    raw_consumption DECIMAL(20,6) NOT NULL,
    billed_consumption DECIMAL(20,6) NOT NULL,
    ct_ratio DECIMAL(12,6),
    pt_ratio DECIMAL(12,6),
    strategy_id BIGINT NOT NULL,
    strategy_code VARCHAR(64),
    strategy_version INT,
    price_type INT,
    unit_price DECIMAL(18,8),
    plus_price DECIMAL(18,8),
    charge_amount DECIMAL(18,4) NOT NULL,
    project_id BIGINT,
    project_name VARCHAR(128),
    area_group_id BIGINT,
    area_name VARCHAR(128),
    category_group_id BIGINT,
    category_name VARCHAR(128),
    detail_json VARCHAR(4096),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted TINYINT DEFAULT 0 NOT NULL
);
