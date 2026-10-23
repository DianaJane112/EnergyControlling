package com.competition.energycontrollerplatform.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 积成协议自定义断帧器（Netty {@link ByteToMessageDecoder}）。
 * <p>
 * 职责：解决 TCP 粘包/拆包问题，从字节流中切分出完整帧并传递给下游 Handler。
 * <p>
 * 帧格式（集中器→主站，返回帧）：
 * <pre>
 * [0xFE][0xFE] [0x68] [区域(1)] [地址(5 BCD)] [功能(1)] [长度(2 LE)] [数据(N)] [ReplyFlag(1)] [SER(1)] [CS(1)] [0x16]
 * </pre>
 * 数据长度(DataLength)位于帧头 0x68 起第8、9字节（低字节在前），
 * 整帧长度 = 14 + DataLength 字节（从 0x68 算起）。
 */
public class JiChengFrameDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(JiChengFrameDecoder.class);

    /** 帧头 */
    private static final byte FRAME_HEAD = (byte) 0x68;
    /** 帧尾 */
    private static final byte FRAME_TAIL = (byte) 0x16;
    /** 引导字节（唤醒码） */
    private static final byte GUIDE_BYTE = (byte) 0xFE;

    /** 最小帧：0x68 + 区域(1) + 地址(5) + 功能(1) + 长度(2) + CS(1) + 帧尾(1) = 12 字节（数据域为 0 时） */
    private static final int MIN_FRAME_LEN = 12;

    /** 从 0x68 到 DataLength 低字节的偏移（含 0x68 自身 1 + 区域 1 + 地址 5 + 功能 1 = 8 字节，DataLen LO 即第 8 字节） */
    private static final int DATA_LEN_OFFSET = 8;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 入站原始字节流 hex dump（联调排查用）
        if (in.readableBytes() > 0 && log.isInfoEnabled()) {
            log.info("[DECODER←TCP] 收到原始字节 {} bytes:\n{}",
                    in.readableBytes(), ByteBufUtil.prettyHexDump(in));
        }

        while (in.readableBytes() > 0) {
            // ① 定位帧头 0x68（可能在引导字节 0xFE 之后）
            int headIdx = findFrameHead(in);
            if (headIdx < 0) {
                // 未找到帧头，跳过所有字节
                in.skipBytes(in.readableBytes());
                return;
            }

            // 跳过帧头之前的字节
            if (headIdx > in.readerIndex()) {
                in.skipBytes(headIdx - in.readerIndex());
            }
            // 现在 in.readerIndex() 指向 0x68

            int frameStart = in.readerIndex();
            int readable = in.readableBytes();

            // ② 至少需要 MIN_FRAME_LEN 字节才能读取 DataLength
            if (readable < MIN_FRAME_LEN) {
                return; // 等待更多数据到达
            }

            // ③ 读取帧头 0x68
            byte head = in.getByte(frameStart);
            if (head != FRAME_HEAD) {
                in.skipBytes(1);
                continue;
            }

            // ④ 读取第 8、9 字节：DataLength（低字节在前，小端序）
            int dataLenLo = in.getByte(frameStart + DATA_LEN_OFFSET) & 0xFF;
            int dataLenHi = in.getByte(frameStart + DATA_LEN_OFFSET + 1) & 0xFF;
            int dataLen = dataLenLo | (dataLenHi << 8);

            // ⑤ 计算整帧长度：头部固定 10 + DataLength + ReplyFlag(1) + SER(1) + CS(1) + 帧尾(1) = 14 + dataLen
            int totalFrameLen = 14 + dataLen; // 从 0x68 算起到 0x16 结束

            // ⑥ 检查是否收到完整帧
            if (readable < totalFrameLen) {
                return; // 等待更多数据
            }

            // ⑦ 校验帧尾 0x16
            int tailIdx = frameStart + totalFrameLen - 1;
            byte tail = in.getByte(tailIdx);
            if (tail != FRAME_TAIL) {
                log.warn("帧尾校验失败: 期望 0x16 实际 0x{}，跳过 1 字节重试",
                        Integer.toHexString(tail & 0xFF));
                in.skipBytes(1);
                continue;
            }

            // ⑧ 校验 CS（累加和校验码）
            // CS = sum(0x68 .. SER) % 256，CS 存储于 SER 之后、0x16 之前
            int csIdx = tailIdx - 1; // CS 在帧尾前 1 字节
            int expectedCs = calculateCs(in, frameStart, dataLen);
            int actualCs = in.getByte(csIdx) & 0xFF;
            if (expectedCs != actualCs) {
                log.warn("CS 校验失败: 计算值 0x{} 实际值 0x{}，跳过 1 字节重试",
                        Integer.toHexString(expectedCs), Integer.toHexString(actualCs));
                in.skipBytes(1);
                continue;
            }

            // ⑨ 提取完整帧（包含 0x68 到 0x16），交给下游 Handler
            ByteBuf frame = in.retainedSlice(frameStart, totalFrameLen);
            out.add(frame);

            // ⑩ 移动 readerIndex 跳过已处理的帧
            in.skipBytes(totalFrameLen);
            // 继续循环，处理缓冲区中可能存在的后续帧
        }
    }

    /**
     * 在 ByteBuf 中查找帧头 0x68 的位置。
     * 帧头前可能包含 0xFE 引导字节。
     *
     * @return 帧头索引，未找到返回 -1
     */
    private int findFrameHead(ByteBuf in) {
        for (int i = in.readerIndex(); i < in.readerIndex() + in.readableBytes(); i++) {
            int b = in.getByte(i) & 0xFF;
            if (b == 0x68) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算累加和校验（CS）。
     * <p>
     * 校验范围：从 0x68（含）到 SER（含），即从 frameStart 到 frameStart + 10 + dataLen + 1 +
     * 1 之前。
     * 固定字段：head(1) + region(1) + addr(5) + func(1) + len(2) = 10 +
     * data + replyFlag(1) + SER(1)
     */
    private int calculateCs(ByteBuf in, int frameStart, int dataLen) {
        int sum = 0;
        // 从 0x68 一直加到 SER（含），共 10 + dataLen + 2 字节
        int csEndOff = frameStart + 10 + dataLen + 2;
        for (int i = frameStart; i < csEndOff; i++) {
            sum += in.getByte(i) & 0xFF;
        }
        return sum & 0xFF;
    }
}
