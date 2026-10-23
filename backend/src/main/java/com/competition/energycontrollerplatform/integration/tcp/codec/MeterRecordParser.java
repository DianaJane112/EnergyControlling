package com.competition.energycontrollerplatform.integration.tcp.codec;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CJ/T 188 普通水表记录解析器。
 * 每条记录 16 字节：
 * <pre>
 * [表类型(1)][表地址(5 BCD)][厂商代码(2)][规约类型(1)][通讯参数(1)]
 * [当前累积流量小数点后两位(1)][低两位(1)][中两位(1)][高两位(1)]
 * [状态字1(1)][状态字2(1)]
 * </pre>
 * 累计流量 = 高两位+中两位+低两位 . 小数点后两位（单位：L）
 */
public final class MeterRecordParser {

    private static final Logger log = LoggerFactory.getLogger(MeterRecordParser.class);

    /** CJ/T 188 普通水表单条记录长度 */
    public static final int RECORD_LEN_188 = 16;

    private MeterRecordParser() {}

    @Data
    public static class MeterRecord {
        private byte meterType;
        private String meterAddr;       // 表计 5 字节 BCD 地址 → 字符串
        private String vendorCode;      // 厂商代码
        private byte protocolType;
        private byte commParam;
        /** 当前累计涌水量（单位：L） */
        private BigDecimal cumulativeFlow;
        private int status1;
        private int status2;
    }

    /**
     * 从字节偏移解析一条 CJ/T 188 普通水表记录
     */
    public static MeterRecord parse188Record(byte[] data, int offset) {
        if (data.length - offset < RECORD_LEN_188) {
            log.warn("数据不足 {} 字节，无法解析 188 表计记录", RECORD_LEN_188);
            return null;
        }
        MeterRecord r = new MeterRecord();

        // 表类型
        r.meterType = data[offset];

        // 表地址 5 字节 BCD
        r.meterAddr = BcdUtils.bcdToString(data, offset + 1, 5);

        // 厂商代码 2 字节
        byte v1 = data[offset + 6];
        byte v2 = data[offset + 7];
        r.vendorCode = String.format("%02X%02X", v1 & 0xFF, v2 & 0xFF);

        // 规约类型
        r.protocolType = data[offset + 8];

        // 通讯参数
        r.commParam = data[offset + 9];

        // 累计流量：小数(1) + 低(1) + 中(1) + 高(1) = 4 字节 BCD
        int flowDecimal = bcdByteToInt(data[offset + 10]);
        int flowLow = bcdByteToInt(data[offset + 11]);
        int flowMid = bcdByteToInt(data[offset + 12]);
        int flowHigh = bcdByteToInt(data[offset + 13]);

        // 拼凑总立方数：高两位是万位+千位，中两位是百位+十位，低两位是个位+小数第一位... 等等
        // 实际协议：小数点后两位→低两位→中两位→高两位 组成 8 位整数 + 2 位小数
        long intPart = ((long) flowHigh * 10000L) + (flowMid * 100L) + flowLow;
        BigDecimal cumulative = BigDecimal.valueOf(intPart)
                .add(BigDecimal.valueOf(flowDecimal, 2));
        r.cumulativeFlow = cumulative.setScale(2, RoundingMode.HALF_UP);

        r.status1 = data[offset + 14] & 0xFF;
        r.status2 = data[offset + 15] & 0xFF;

        return r;
    }

    private static int bcdByteToInt(byte b) {
        return ((b >> 4) & 0x0F) * 10 + (b & 0x0F);
    }
}
