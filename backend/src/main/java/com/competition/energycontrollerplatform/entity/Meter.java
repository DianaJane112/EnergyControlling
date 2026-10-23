package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_meter")
public class Meter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("customer_id")
    private Long customerId;

    /** 表计性质：PHYSICAL=真实表，VIRTUAL=虚拟结算表 */
    @TableField("meter_kind")
    private String meterKind;

    /** 关联分组ID（外键→t_meter_group.id），支撑按分组统计用电量 */
    @TableField("group_id")
    private Long groupId;

    // 积成水表字段
    @TableField("steal_no")
    private String stealNo;

    private String vender;

    @TableField("meter_type_name")
    private String meterTypeName;

    private String caliber;

    @TableField("switch_op")
    private String switchOp;

    @TableField("start_val")
    private String startVal;

    @TableField("terminal_name")
    private String terminalName;

    @TableField("channel_type")
    private String channelType;

    @TableField("meter_phone")
    private String meterPhone;

    @TableField("telecomm_sn")
    private String telecommSn;

    private String imei;

    private String iccid;

    private String address;

    @TableField("create_date")
    private LocalDateTime createDate;

    private String type;

    private String sign;

    // 辰鳗电表字段
    @TableField("db_no")
    private String dbNo;

    private String name;

    private Integer bei;

    private Integer bjlx;

    /** 表计类型：0=水表, 1=电表 */
    @TableField("meter_type")
    private Integer meterType;

    private BigDecimal credit;

    private BigDecimal warnline;

    private String creatuser;

    private String cttime;

    private Integer feetype;

    @TableField("pro_id")
    private Long proId;

    @TableField("pro_name")
    private String proName;

    @TableField("plus_price")
    private BigDecimal plusPrice;

    @TableField("price_type")
    private Integer priceType;

    @TableField("price_content")
    private String priceContent;

    @TableField("join_phone")
    private String joinPhone;

    @TableField("collector_device_no")
    private String collectorDeviceNo;

    private Integer phase;

    // 捷先水表字段
    private String meteraddress;

    private String companycode;

    @TableField("water_vendor_id")
    private Long waterVendorId;

    @TableField("customerno")
    private String customerNo;

    @TableField("meter_caliber")
    private String meterCaliber;

    private Integer controlling;

    @TableField("state_valve")
    private Integer stateValve;

    @TableField("signal_strength")
    private Integer signalStrength;

    @TableField("battery_voltage")
    private BigDecimal batteryVoltage;

    // 结算追踪（自动扣费用）
    @TableField("last_settled_reading")
    private BigDecimal lastSettledReading;

    @TableField("last_settled_time")
    private LocalDateTime lastSettledTime;

    /** 关联费率策略ID（外键→t_price_strategy.id） */
    @TableField("rate_config_id")
    private Long rateConfigId;

    // CT/PT变比
    @TableField("ct_ratio")
    private java.math.BigDecimal ctRatio;

    @TableField("pt_ratio")
    private java.math.BigDecimal ptRatio;

    // 标记状态与告警
    @TableField("mark_status")
    private String markStatus;

    @TableField("enabled_time")
    private LocalDateTime enabledTime;

    @TableField("disable_warning")
    private Integer disableWarning;

    @TableField("comm_status_desc")
    private String commStatusDesc;

    // 通用状态
    private Integer status;

    private Integer online;

    @TableField("meter_signal")
    private Integer meterSignal;

    // 通信参数（昊普电表表单配置）
    @TableField("collectorCode")
    private String collectorCode;

    /** 昊普电表唯一设备编号，对应接口一/接口二中的 meterCode */
    @TableField("meterCode")
    private Long meterCode;

    @TableField("bpsType")
    private String bpsType;

    @TableField("bitType")
    private String bitType;

    @TableField("protoType")
    private String protoType;

    private Integer serial;

    @TableField("deviceType")
    private String deviceType;

    // 审计字段
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
