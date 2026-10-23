package com.competition.energycontrollerplatform.integration.tcp.codec;

/**
 * BCD 码编解码工具。
 * 用于积成水表 5 字节 BCD 地址的转换。
 */
public final class BcdUtils {

    private BcdUtils() {}

    /**
     * BCD 字节数组 → 十进制字符串（每字节 2 位，不足补 0）
     */
    public static String bcdToString(byte[] bcd, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02X", bcd[i] & 0xFF));
        }
        // 去掉前导零（至少保留一位）
        String result = sb.toString().replaceFirst("^0+", "");
        return result.isEmpty() ? "0" : result;
    }

    /**
     * BCD 字节数组 → 十进制字符串（保留前导零）
     */
    public static String bcdToStringPadded(byte[] bcd, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02X", bcd[i] & 0xFF));
        }
        return sb.toString();
    }
}
