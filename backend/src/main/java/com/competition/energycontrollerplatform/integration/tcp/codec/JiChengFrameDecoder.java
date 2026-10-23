package com.competition.energycontrollerplatform.integration.tcp.codec;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 积成水表二进制帧解码器。
 * 负责断帧、头尾校验、CS 校验和、提取各字段。
 * <p>
 * 帧格式（集中器→主站，发送帧）：
 * <pre>
 * [0xFE][0xFE][0x68][区域(1)][地址(5 BCD)][功能(1)][长度(2 LE)][数据...][CS(1)][0x16]
 * </pre>
 * 返回帧额外包含 [ReplyFlag][SER] 在数据段之后。
 */
public class JiChengFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(JiChengFrameDecoder.class);

    public static final byte FRAME_HEAD = (byte) 0x68;
    public static final byte FRAME_TAIL = (byte) 0x16;
    public static final byte GUIDE_BYTE = (byte) 0xFE;

    /**
     * 尝试从 ByteBuf 解析一帧。未找到完整帧返回 null。
     * 调用前确保 buf.readableBytes() >= 10（最小帧：68+区域+5地址+功能+2长度+CS+16 = 11 + 前2引导 = 13）
     */
    public static JiChengFrame decode(ByteBuf buf) {
        int readerIndex = buf.readerIndex();
        int readable = buf.readableBytes();

        // 至少需要 2 引导 + 11 最小帧 = 13 字节
        if (readable < 13) return null;
        buf.markReaderIndex();

        try {
            // 找到帧头序列 [0xFE, 0xFE, 0x68]
            int frameStart = indexOf3(buf, readerIndex, readable);
            if (frameStart < 0) {
                // 找不到，跳过所有可读字节
                buf.skipBytes(readable);
                return null;
            }

            // 跳过已确认的非帧字节来到第一个 0xFE
            int skipped = frameStart - readerIndex;
            if (skipped > 0) buf.skipBytes(skipped);

            // 跳过 0xFE 0xFE
            buf.skipBytes(2);

            // 现在 buf 指向 0x68 的位置
            int frameOffset = buf.readerIndex(); // 0x68 的位置
            int remaining = buf.readableBytes();
            if (remaining < 11) { // head(1) + region(1) + addr(5) + func(1) + len(2) + cs(1) + tail(1) = at least 12
                buf.resetReaderIndex();
                return null;
            }

            byte head = buf.readByte();
            if (head != FRAME_HEAD) {
                buf.readerIndex(frameOffset + 1);
                return null;
            }

            // 区域识别码
            byte region = buf.readByte();

            // 5 字节 BCD 地址
            byte[] addr = new byte[5];
            buf.readBytes(addr, 0, 5);

            // 功能码
            byte func = buf.readByte();

            // 数据长度（2 字节 LE）
            int dataLen = buf.readUnsignedShortLE();

            // 检查是否有完整的数据 + CS + 帧尾
            if (buf.readableBytes() < dataLen + 1 + 1) { // data + cs + tail
                buf.resetReaderIndex();
                return null;
            }

            // 数据域
            byte[] data = new byte[dataLen];
            if (dataLen > 0) buf.readBytes(data, 0, dataLen);

            // 提取回复标志和 SER（功能码与发送帧同为 0x15 的返回帧数据域后有 REPLY_FLAG + SER）
            byte replyFlag = 0;
            byte ser = 0;
            // 判定规则：GPRS 模式下集中器主动上报的可能是返回帧
            // 尝试判断数据域后是否直接有 replyFlag+SER（非 heartbeat）
            if (func != JiChengFrame.FUNC_HEARTBEAT && isReplyFrame(data, func)) {
                // 返回帧格式：CS 在 replyFlag+SER 之后
                // 但这里的 data 已经读了 dataLen 字节，需回退处理
                // 简化为：当前已读 dataLen 字节后的下一个是 REPLY_FLAG + SER，再是 CS
                // 标准返回帧数据域通常不含 replyFlag+SER，它们在数据域之后
                // 我们的处理：检查 buf 当前剩余是否足够
                if (buf.readableBytes() >= 3) { // replyFlag(1) + ser(1) + cs(1)
                    replyFlag = buf.readByte();
                    ser = buf.readByte();
                }
            }

            // CS 校验和
            byte cs = buf.readByte();

            // 帧尾
            byte tail = buf.readByte();
            if (tail != FRAME_TAIL) {
                log.warn("帧尾校验失败: 期望 0x16 收到 0x{}", Integer.toHexString(tail & 0xFF));
                return null;
            }

            // 计算校验和
            int expectedCs = calculateCs(buf, frameOffset, dataLen, replyFlag, ser);
            if ((cs & 0xFF) != (expectedCs & 0xFF)) {
                log.warn("CS 校验失败: 计算值 0x{} 实际值 0x{}",
                        Integer.toHexString(expectedCs & 0xFF), Integer.toHexString(cs & 0xFF));
                // 帧尾正确但 CS 失败 —— 记录日志但继续处理（容错）
            }

            JiChengFrame frame = new JiChengFrame();
            frame.setRegionCode(region & 0xFF);
            frame.setAddr(addr);
            frame.setFuncCode(func);
            frame.setData(data);
            frame.setReplyFlag(replyFlag);
            frame.setSer(ser);
            frame.setReply(replyFlag != 0 || (func & 0x80) == 0x80);
            return frame;

        } catch (Exception e) {
            log.error("帧解码异常: {}", e.getMessage());
            buf.resetReaderIndex();
            return null;
        }
    }

    /**
     * 在 buf 中查找 [0xFE, 0xFE, 0x68] 序列
     */
    private static int indexOf3(ByteBuf buf, int from, int max) {
        for (int i = from; i + 2 < from + max; i++) {
            if ((buf.getByte(i) & 0xFF) == 0xFE
                    && (buf.getByte(i + 1) & 0xFF) == 0xFE
                    && (buf.getByte(i + 2) & 0xFF) == 0x68) {
                return i;
            }
        }
        return -1;
    }

    /**
     * CS 校验和 = 从 0x68 开始到 CS 字节之前的所有字节累加和低 8 位
     */
    private static int calculateCs(ByteBuf buf, int frameOffset, int dataLen, byte replyFlag, byte ser) {
        int sum = 0;
        // 0x68(1) + region(1) + addr(5) + func(1) + len(2) + data(dataLen)
        int csEnd = frameOffset + 1 + 1 + 5 + 1 + 2 + dataLen;
        for (int i = frameOffset; i < csEnd; i++) {
            sum += buf.getUnsignedByte(i);
        }
        // 返回帧还需要加上 replyFlag + SER
        if (replyFlag != 0 || ser != 0) {
            sum += replyFlag & 0xFF;
            sum += ser & 0xFF;
        }
        return sum & 0xFF;
    }

    /**
     * 粗略判断是否为返回帧（发送帧数据域后直接是 CS，不是 replyFlag+SER）
     */
    private static boolean isReplyFrame(byte[] data, byte func) {
        // 功能码 0x15/0x44 返回帧通常包含 reply flag
        return func == JiChengFrame.FUNC_READ_ALL_CURRENT
                || func == JiChengFrame.FUNC_READ_SINGLE_REALTIME;
    }
}
