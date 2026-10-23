package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表计详情响应 —— 按来源分组
 */
@Data
public class MeterResponse {

    private Long id;
    /** 表计性质：PHYSICAL=真实表，VIRTUAL=虚拟结算表 */
    private String meterKind;
    private Long customerId;
    private String customerName;   // 关联查询填充
    private BigDecimal customerBalance;  // 关联查询填充（商户账户余额）
    /** 今日有功电量：当天累计有功电量最大值减最小值；读数不足两条时为 null。 */
    private BigDecimal todayActiveEnergy;

    /** 最新上报的累计正向有功电量（当前表底）。 */
    private BigDecimal currentActiveEnergy;

    /** 最新一条有效读数，详情接口统一返回，避免前端再依赖曲线接口补充功率。 */
    private LatestReading latestReading;

    @Data
    public static class LatestReading {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime timestamp;
        /** 总有功功率（W） */
        private BigDecimal power;
        /** 组合有功总电量（kWh） */
        private BigDecimal kwh;
        /** 功率因数 */
        private BigDecimal fact;
        /** 反向有功电量 */
        private BigDecimal negativeActive;
        /** 组合有功电量 */
        private BigDecimal combinedActive;
        /** 尖时段电量 */
        private BigDecimal kwhPeak;
        /** 峰时段电量 */
        private BigDecimal kwhBusy;
        /** 平时段电量 */
        private BigDecimal kwhNormal;
        /** 谷时段电量 */
        private BigDecimal kwhIdle;
        /** A相电流 */
        private BigDecimal ampA;
        /** B相电流 */
        private BigDecimal ampB;
        /** C相电流 */
        private BigDecimal ampC;
        /** 上报间隔（分） */
        private Integer timeInterval;
    }

    /** 积成水表信息 */
    private Jicheng jicheng;

    /** 辰鳗电表信息 */
    private Chenman chenman;

    /** 捷先水表信息 */
    private Jiexian jiexian;

    /** 通用状态 */
    private General general;

    /** 通信参数 */
    private CommParams commParams;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    private String createBy;

    // ──── 嵌套分组 ────

    @Data
    public static class Jicheng {
        private String stealNo;
        private String vender;
        private String meterTypeName;
        private String caliber;
        private String switchOp;
        private String startVal;
        private String terminalName;
        private String channelType;
        private String meterPhone;
        private String telecommSn;
        private String imei;
        private String iccid;
        private String address;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime createDate;

        private String type;
        private String sign;
    }

    @Data
    public static class Chenman {
        private String dbNo;
        private String name;
        private Integer bei;
        private Integer bjlx;
        private BigDecimal credit;
        private BigDecimal warnline;
        private String creatuser;
        private String cttime;
        private Integer feetype;
        private Long proId;
        private String proName;
        private BigDecimal plusPrice;
        private Integer priceType;
        private String priceContent;
        private String joinPhone;
        private String collectorDeviceNo;
        private Integer phase;
    }

    @Data
    public static class Jiexian {
        private String meteraddress;
        private String companycode;
        private String customerNo;
        private String meterCaliber;
        private Integer controlling;
        private Integer stateValve;
        private Integer signalStrength;
        private BigDecimal batteryVoltage;
    }

    @Data
    public static class General {
        private Integer status;
        private Integer online;
        private Integer meterSignal;
        /** 表计类型：0=水表, 1=电表 */
        private Integer meterType;
        private Long rateConfigId;
        private Long groupId;
        private String groupName;
        private String groupPath;
        private Integer groupStatus;
        private String groupRelationStatus;
        private Long waterVendorId;
        /** CT变比 */
        private java.math.BigDecimal ctRatio;
        /** PT变比 */
        private java.math.BigDecimal ptRatio;
        /** 标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE */
        private String markStatus;
        /** 启用时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private java.time.LocalDateTime enabledTime;
        /** 是否停用警告：0-启用, 1-停用 */
        private Integer disableWarning;
        /** 通讯状态描述 */
        private String commStatusDesc;
    }

    @Data
    public static class CommParams {
        private String collectorCode;
        private String bpsType;
        private String bitType;
        private String protoType;
        private Integer serial;
        private String deviceType;
    }
}
