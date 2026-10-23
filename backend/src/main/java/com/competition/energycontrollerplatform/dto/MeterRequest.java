package com.competition.energycontrollerplatform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 表计新增/编辑请求
 */
@Data
public class MeterRequest {

    private Long customerId;

    /** 商户名称（与 customerId 二选一，优先使用此字段自动查询商户ID） */
    private String customerName;

    /** 关联分组ID（外键→t_meter_group.id），支撑按分组统计用电量 */
    private Long groupId;

    // ── 积成水表字段 ──

    /** 表钢印号（水表标识，电表使用dbNo） */
    private String stealNo;

    /** 生产厂家 */
    private String vender;

    /** 表类型名称 */
    private String meterTypeName;

    /** 表口径 */
    private String caliber;

    /** 阀控标识：1独立表计/2表加阀 */
    private String switchOp;

    /** 新表底度 */
    private String startVal;

    /** 终端名称 */
    private String terminalName;

    /** 通道类型 */
    private String channelType;

    /** 表计电话 */
    private String meterPhone;

    /** 通讯序号 */
    private String telecommSn;

    /** IMEI码 */
    private String imei;

    /** ICCID */
    private String iccid;

    /** 表计安装地址 */
    private String address;

    /** 建档类型：0新建/1修改/2删除 */
    private String type;

    // ── 辰鳗电表字段 ──

    /** 电表表号 */
    private String dbNo;

    /** 商户名 */
    private String name;

    /** 倍率 */
    private Integer bei;

    /** 表计类型：1租户/2总表/3公共/4其他/5分摊表/6公区总表（辰鳗bjlx） */
    private Integer bjlx;

    /** 表计类型：0=水表, 1=电表 */
    private Integer meterType;

    /** 透支额度 */
    private BigDecimal credit;

    /** 告警额度 */
    private BigDecimal warnline;

    /** 安装人 */
    private String creatuser;

    /** 安装时间 */
    private String cttime;

    /** 付费方式：1预付费/2后付费 */
    private Integer feetype;

    /** 项目ID */
    private Long proId;

    /** 项目名称 */
    private String proName;

    /** 附加电费 */
    private BigDecimal plusPrice;

    /** 电价类型 */
    private Integer priceType;

    /** 电价内容JSON */
    private String priceContent;

    /** 关联用户电话JSON */
    private String joinPhone;

    /** 采集器号 */
    private String collectorDeviceNo;

    /** 相数：1单相/3三相 */
    private Integer phase;

    // ── 捷先水表字段 ──

    /** 水表编号 */
    private String meteraddress;

    /** 厂家编号 */
    private String companycode;

    private Long waterVendorId;

    /** 营收户号 */
    private String customerNo;

    /** 水表口径 */
    private String meterCaliber;

    /** 是否有阀控：0无/1有 */
    private Integer controlling;

    /** 阀门状态 */
    private Integer stateValve;

    /** 信号强度 */
    private Integer signalStrength;

    /** 电池电压 */
    private BigDecimal batteryVoltage;

    // ── 通用状态 ──

    /** 运行状态 */
    private Integer status;

    /** 在线状态 */
    private Integer online;

    /** 信号强度0-30（辰鳗） */
    private Integer meterSignal;

    // ── CT/PT变比 ──

    /** CT变比（电流互感器） */
    private java.math.BigDecimal ctRatio;

    /** PT变比（电压互感器） */
    private java.math.BigDecimal ptRatio;

    // ── 标记与告警 ──

    /** 标记状态：NORMAL-正常, ATTENTION-关注, ABNORMAL-异常, MAINTENANCE-待维护 */
    private String markStatus;

    /** 启用时间 */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private java.time.LocalDateTime enabledTime;

    /** 是否停用警告：0-启用告警, 1-停用告警 */
    private Integer disableWarning;

    /** 通讯状态描述 */
    private String commStatusDesc;

    // ── 通信参数 ──

    /** 控制器号 */
    private String collectorCode;

    /** 波特率 */
    private String bpsType;

    /** 校验位 */
    private String bitType;

    /** 协议类型 */
    private String protoType;

    /** 串口号 */
    private Integer serial;

    /** 设备类型 */
    private String deviceType;

    // ── 费率策略 ──

    /** 关联费率策略ID（外键→t_price_strategy.id） */
    private Long rateConfigId;
}
