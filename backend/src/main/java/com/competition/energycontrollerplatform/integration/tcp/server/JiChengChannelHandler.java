package com.competition.energycontrollerplatform.integration.tcp.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.integration.tcp.codec.*;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积成水表 Netty 通道处理器。
 * 处理集中器连接、心跳、二进制帧解析与数据持久化。
 */
public class JiChengChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(JiChengChannelHandler.class);

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final String remoteAddr;

    public JiChengChannelHandler(MeterMapper meterMapper,
                                  MeterReadingMapper meterReadingMapper,
                                  String remoteAddr) {
        this.meterMapper = meterMapper;
        this.meterReadingMapper = meterReadingMapper;
        this.remoteAddr = remoteAddr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("集中器 TCP 连接已建立: {}", remoteAddr);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("集中器 TCP 连接断开: {}", remoteAddr);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (!(msg instanceof io.netty.buffer.ByteBuf buf)) {
                return;
            }

            // 循环解码（一个 TCP 包可能包含多帧）
            while (buf.readableBytes() >= 13) {
                int before = buf.readerIndex();
                JiChengFrame frame = JiChengFrameDecoder.decode(buf);
                if (frame == null) {
                    // 没找到完整帧，退出等待下次数据
                    if (buf.readerIndex() == before) {
                        break;
                    }
                    continue;
                }
                handleFrame(frame);
            }
            // 清理已读字节
            buf.discardReadBytes();
        } catch (Exception e) {
            log.error("处理集中器报文异常 remote={}: {}", remoteAddr, e.getMessage());
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("集中器通道异常 remote={}: {}", remoteAddr, cause.getMessage());
        ctx.close();
    }

    // ──────── 帧分发 ────────

    private void handleFrame(JiChengFrame frame) {
        String collAddr = BcdUtils.bcdToString(frame.getAddr(), 0, 5);
        log.debug("收到集中器帧 addr={} func=0x{} dataLen={}",
                collAddr, Integer.toHexString(frame.getFuncCode() & 0xFF),
                frame.getData() != null ? frame.getData().length : 0);

        switch (frame.getFuncCode()) {
            case JiChengFrame.FUNC_READ_ALL_CURRENT:   // 0x15
            case JiChengFrame.FUNC_READ_SINGLE_REALTIME: // 0x44
                handleMeterData(frame, collAddr);
                break;
            default:
                log.debug("未处理的功能码 0x{} 来自集中器 addr={}",
                        Integer.toHexString(frame.getFuncCode() & 0xFF), collAddr);
                break;
        }
    }

    private void handleMeterData(JiChengFrame frame, String collAddr) {
        byte[] data = frame.getData();
        if (data == null || data.length < MeterRecordParser.RECORD_LEN_188) {
            log.debug("数据域过短 (len={})，非 188 表计数据", data != null ? data.length : 0);
            return;
        }

        long nowTs = System.currentTimeMillis();
        int recordCount = 0;
        int offset = 0;

        while (offset + MeterRecordParser.RECORD_LEN_188 <= data.length) {
            MeterRecordParser.MeterRecord record =
                    MeterRecordParser.parse188Record(data, offset);
            offset += MeterRecordParser.RECORD_LEN_188;

            if (record == null) continue;

            // 根据表计地址匹配本地 t_meter
            Meter meter = findByStealNo(record.getMeterAddr());
            if (meter == null) {
                log.debug("未匹配到表计 stealNo={}, 集中器={}", record.getMeterAddr(), collAddr);
                continue;
            }

            // 写入库
            MeterReading r = new MeterReading();
            r.setMeterId(meter.getId());
            r.setStealNo(record.getMeterAddr());
            r.setTimestamp(LocalDateTime.now());
            r.setReadVal(record.getCumulativeFlow());
            // 将累计流量（L）转成立方米存入 kwh 字段同系列位置
            r.setKwh(record.getCumulativeFlow()
                    .divide(BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP));
            r.setCreateTime(LocalDateTime.now());
            r.setUpdateTime(LocalDateTime.now());

            try {
                meterReadingMapper.insert(r);
                recordCount++;
            } catch (Exception e) {
                log.error("写库失败 meterId={} stealNo={}: {}",
                        meter.getId(), record.getMeterAddr(), e.getMessage());
            }
        }

        if (recordCount > 0) {
            log.info("集中器 {} 上报 {} 条表计数目已入库", collAddr, recordCount);
        }
    }

    private Meter findByStealNo(String stealNo) {
        if (stealNo == null || stealNo.isBlank() || "0".equals(stealNo)) return null;
        return meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>().eq(Meter::getStealNo, stealNo));
    }
}
