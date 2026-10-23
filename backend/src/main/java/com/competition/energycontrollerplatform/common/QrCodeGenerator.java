package com.competition.energycontrollerplatform.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * 二维码生成工具。
 * <p>
 * 基于 Google ZXing 生成 PNG 格式二维码图片字节数组，
 * 用于电表/水表二维码打印标签及扫码绑定场景。
 * <p>
 * 二维码内容格式：{@code METER:<type>:<dbNo>}
 * <ul>
 *   <li>type: 0=水表, 1=电表</li>
 *   <li>dbNo: 表计编号（与 t_meter.db_no 对应）</li>
 * </ul>
 */
public final class QrCodeGenerator {

    /** 默认二维码图片尺寸（300×300 像素） */
    private static final int DEFAULT_SIZE = 300;

    /** 输出图片格式 */
    private static final String IMAGE_FORMAT = "PNG";

    /** 二维码内容前缀（用于扫码端识别） */
    private static final String PREFIX = "METER:";

    private QrCodeGenerator() {
        // 工具类不允许实例化
    }

    /**
     * 生成默认尺寸（300×300）的二维码 PNG 图片字节数组。
     *
     * @param content 二维码文本内容
     * @return PNG 格式字节数组
     */
    public static byte[] generate(String content) {
        return generate(content, DEFAULT_SIZE);
    }

    /**
     * 生成二维码 PNG 图片字节数组。
     *
     * @param content 二维码文本内容
     * @param size    图片尺寸（像素，正方形）
     * @return PNG 格式字节数组
     */
    public static byte[] generate(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, IMAGE_FORMAT, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("二维码生成失败，请稍后重试");
        }
    }

    /**
     * 根据表计类型和编号生成二维码内容字符串。
     * <p>
     * 格式：{@code METER:<meterType>:<dbNo>}
     *
     * @param meterType 表计类型（0=水表, 1=电表）
     * @param dbNo      表计编号
     * @return 二维码内容字符串
     */
    public static String buildContent(Integer meterType, String dbNo) {
        return PREFIX + meterType + ":" + (dbNo != null ? dbNo.trim() : "");
    }

    /**
     * 解析二维码内容，提取表计类型和编号。
     *
     * @param qrContent 二维码扫描内容
     * @return {meterType, dbNo}，格式不匹配时返回 null
     */
    public static String[] parseContent(String qrContent) {
        if (qrContent == null || qrContent.isBlank()) {
            return null;
        }
        String content = qrContent.trim();
        if (!content.startsWith(PREFIX)) {
            return null;
        }
        // 去掉 "METER:" 前缀，按 ":" 切分 type 和 dbNo
        String payload = content.substring(PREFIX.length());
        int colonIdx = payload.indexOf(':');
        if (colonIdx <= 0 || colonIdx >= payload.length() - 1) {
            return null;
        }
        String typeStr = payload.substring(0, colonIdx);
        String dbNo = payload.substring(colonIdx + 1);
        if (!typeStr.equals("0") && !typeStr.equals("1")) {
            return null;
        }
        return new String[]{typeStr, dbNo};
    }
}
