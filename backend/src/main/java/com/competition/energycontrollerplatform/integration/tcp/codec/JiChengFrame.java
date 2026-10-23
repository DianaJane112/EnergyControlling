package com.competition.energycontrollerplatform.integration.tcp.codec;

import lombok.Data;

/**
 * 积成水表帧模型。
 * 从 TCP 字节流中解析出的标准化帧结构。
 */
@Data
public class JiChengFrame {

    /** 功能码：读取所有表计当前数据（0x15） */
    public static final byte FUNC_READ_ALL_CURRENT = 0x15;
    /** 功能码：读取单表实时数据（0x44） */
    public static final byte FUNC_READ_SINGLE_REALTIME = 0x44;
    /** 功能码：心跳帧 */
    public static final byte FUNC_HEARTBEAT = (byte) 0xAA;

    /** 区域识别码（GPRS 模式为 0xEE，采集器级号为 1-100） */
    private int regionCode;
    /** 集中器 5 字节 BCD 地址 */
    private byte[] addr = new byte[5];
    /** 功能码 */
    private byte funcCode;
    /** 数据域原始字节 */
    private byte[] data;
    /** 回复标志（仅返回帧有，0x00 正常 / 0xCC 异常） */
    private byte replyFlag;
    /** 序列号 */
    private byte ser;
    /** 是否为返回帧（区分发送帧和返回帧的标志） */
    private boolean isReply;
}
