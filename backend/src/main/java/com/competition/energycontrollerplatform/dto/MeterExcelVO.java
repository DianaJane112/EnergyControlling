package com.competition.energycontrollerplatform.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表计 Excel 导出模型（扁平化所有字段）。
 * <p>
 * 将积成/辰鳗/捷先三系统、通用状态、通信参数全部摊平为单行，
 * 使用 EasyExcel 注解定义列头和格式。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class MeterExcelVO {

    // ── 基本信息 ──

    @ExcelProperty("表计ID")
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty("商户名")
    @ColumnWidth(18)
    private String customerName;

    @ExcelProperty("分组ID")
    @ColumnWidth(12)
    private Long groupId;

    @ExcelProperty("表计类型")
    @ColumnWidth(12)
    private String meterTypeDesc;

    // ── 积成水表 ──

    @ExcelProperty("钢印号")
    @ColumnWidth(16)
    private String stealNo;

    @ExcelProperty("生产厂家")
    @ColumnWidth(18)
    private String vender;

    @ExcelProperty("表类型名")
    @ColumnWidth(16)
    private String meterTypeName;

    @ExcelProperty("口径")
    @ColumnWidth(10)
    private String caliber;

    @ExcelProperty("开关阀操作")
    @ColumnWidth(14)
    private String switchOp;

    @ExcelProperty("起始值")
    @ColumnWidth(14)
    private String startVal;

    @ExcelProperty("终端/集中器名称")
    @ColumnWidth(20)
    private String terminalName;

    @ExcelProperty("通道类型")
    @ColumnWidth(12)
    private String channelType;

    @ExcelProperty("表计电话")
    @ColumnWidth(18)
    private String meterPhone;

    @ExcelProperty("通信序列号")
    @ColumnWidth(16)
    private String telecommSn;

    @ExcelProperty("IMEI")
    @ColumnWidth(20)
    private String imei;

    @ExcelProperty("ICCID")
    @ColumnWidth(22)
    private String iccid;

    @ExcelProperty("安装地址")
    @ColumnWidth(30)
    private String address;

    @ExcelProperty("积成创建日期")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createDate;

    // ── 辰鳗电表 ──

    @ExcelProperty("电表号")
    @ColumnWidth(16)
    private String dbNo;

    @ExcelProperty("表计名称")
    @ColumnWidth(20)
    private String name;

    @ExcelProperty("倍率")
    @ColumnWidth(10)
    private Integer bei;

    @ExcelProperty("报警类型")
    @ColumnWidth(12)
    private Integer bjlx;

    @ExcelProperty("信用额度")
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal credit;

    @ExcelProperty("告警线")
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal warnline;

    @ExcelProperty("创建用户")
    @ColumnWidth(14)
    private String creatuser;

    @ExcelProperty("辰鳗创建时间")
    @ColumnWidth(20)
    private String cttime;

    @ExcelProperty("费用类型")
    @ColumnWidth(12)
    private Integer feetype;

    @ExcelProperty("项目ID")
    @ColumnWidth(12)
    private Long proId;

    @ExcelProperty("项目名称")
    @ColumnWidth(24)
    private String proName;

    @ExcelProperty("附加电价")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(14)
    private BigDecimal plusPrice;

    @ExcelProperty("电价类型")
    @ColumnWidth(12)
    private Integer priceType;

    @ExcelProperty("电价内容")
    @ColumnWidth(30)
    private String priceContent;

    @ExcelProperty("联系电话")
    @ColumnWidth(18)
    private String joinPhone;

    @ExcelProperty("集中器设备号")
    @ColumnWidth(20)
    private String collectorDeviceNo;

    @ExcelProperty("相数")
    @ColumnWidth(10)
    private Integer phase;

    // ── 捷先水表 ──

    @ExcelProperty("表计地址")
    @ColumnWidth(30)
    private String meteraddress;

    @ExcelProperty("公司代码")
    @ColumnWidth(14)
    private String companycode;

    @ExcelProperty("客户号")
    @ColumnWidth(18)
    private String customerNo;

    @ExcelProperty("表计口径")
    @ColumnWidth(12)
    private String meterCaliber;

    @ExcelProperty("控阀状态")
    @ColumnWidth(12)
    private Integer controlling;

    @ExcelProperty("阀门状态")
    @ColumnWidth(12)
    private Integer stateValve;

    @ExcelProperty("信号强度")
    @ColumnWidth(12)
    private Integer signalStrength;

    @ExcelProperty("电池电压")
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal batteryVoltage;

    // ── CT/PT变比 ──

    @ExcelProperty("CT变比")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(14)
    private BigDecimal ctRatio;

    @ExcelProperty("PT变比")
    @NumberFormat("#,##0.0000")
    @ColumnWidth(14)
    private BigDecimal ptRatio;

    // ── 标记与告警 ──

    @ExcelProperty("标记状态")
    @ColumnWidth(14)
    private String markStatus;

    @ExcelProperty("启用时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime enabledTime;

    @ExcelProperty("停用警告")
    @ColumnWidth(12)
    private String disableWarningDesc;

    @ExcelProperty("通讯状态")
    @ColumnWidth(16)
    private String commStatusDesc;

    // ── 通用状态 ──

    @ExcelProperty("运行状态")
    @ColumnWidth(12)
    private String statusDesc;

    @ExcelProperty("在线状态")
    @ColumnWidth(12)
    private String onlineDesc;

    @ExcelProperty("表计信号")
    @ColumnWidth(12)
    private Integer meterSignal;

    // ── 通信参数 ──

    @ExcelProperty("集中器编号")
    @ColumnWidth(18)
    private String collectorCode;

    @ExcelProperty("波特率类型")
    @ColumnWidth(14)
    private String bpsType;

    @ExcelProperty("数据位")
    @ColumnWidth(12)
    private String bitType;

    @ExcelProperty("协议类型")
    @ColumnWidth(14)
    private String protoType;

    @ExcelProperty("串口号")
    @ColumnWidth(10)
    private Integer serial;

    @ExcelProperty("设备类型")
    @ColumnWidth(12)
    private String deviceType;

    // ── 审计 ──

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime updateTime;

    @ExcelProperty("创建人")
    @ColumnWidth(14)
    private String createBy;
}
