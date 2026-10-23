package com.competition.energycontrollerplatform.netty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.integration.tcp.codec.BcdUtils;
import com.competition.energycontrollerplatform.integration.tcp.codec.MeterRecordParser;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 积成协议 Netty 通道处理器。
 * <p>
 * 继承 {@link SimpleChannelInboundHandler}，类型参数为 {@link ByteBuf}，
 * 由上游 {@link JiChengFrameDecoder} 保证每次回调传入一帧完整数据。
 * <p>
 * 核心职责：
 * <ol>
 *   <li>解析帧头字段（区域码、5字节BCD集中器地址、功能码、回复标志、SER）</li>
 *   <li>提取 CJ/T 188 表计记录（16字节/条），解析表地址与累计流量</li>
 *   <li>通过表钢印号匹配本地 t_meter 记录</li>
 *   <li>通过独立业务线程池异步写入 t_meter_reading，不阻塞 Netty Worker I/O 线程</li>
 * </ol>
 */
public class JiChengProtocolHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(JiChengProtocolHandler.class);

    // ── 帧字段偏移（从 0x68 起，0-indexed） ──
    private static final int OFF_REGION = 0;       // 区域识别码（0x68 本身）
    private static final int OFF_ADDR = 1;          // 集中器地址（距 0x68 1 字节，共 5 字节 BCD）
    private static final int OFF_FUNC = 6;          // 功能码（距 0x68 6 字节）
    private static final int OFF_DATA_LEN = 7;      // 数据长度（距 0x68 7 字节，2 字节 LE）
    private static final int OFF_DATA = 9;          // 数据域起始（距 0x68 9 字节）

    /** 功能码：读取所有表计当前数据 */
    private static final byte FUNC_READ_ALL_CURRENT = 0x15;
    /** 功能码：读取单表实时数据 */
    private static final byte FUNC_READ_SINGLE_REALTIME = 0x44;

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final ChannelRegistry channelRegistry;
    private final ExecutorService businessExecutor;

    /**
     * 业务线程池配置：
     * <ul>
     *   <li>核心线程 = 2，最大线程 = 8</li>
     *   <li>队列容量 = 2000（超过后拒绝并告警，防止 OOM）</li>
     *   <li>线程名 = jicheng-business-，便于 jstack 排查</li>
     * </ul>
     */
    public JiChengProtocolHandler(MeterMapper meterMapper, MeterReadingMapper meterReadingMapper,
                                   ChannelRegistry channelRegistry) {
        this.meterMapper = meterMapper;
        this.meterReadingMapper = meterReadingMapper;
        this.channelRegistry = channelRegistry;
        this.businessExecutor = new ThreadPoolExecutor(
                2, 8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadPoolExecutor.CallerRunsPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.error("[Netty业务线程池] 队列已满(>2000)，任务被拒绝！请检查数据库写入速度");
                    }
                });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String remote = ctx.channel().remoteAddress() != null
                ? ctx.channel().remoteAddress().toString() : "unknown";
        log.info("集中器 TCP 连接已建立: {}", remote);
        // 通道注册在首次收到 0x15 帧（携带 BCD 地址）时完成
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String remote = ctx.channel().remoteAddress() != null
                ? ctx.channel().remoteAddress().toString() : "unknown";
        log.info("集中器 TCP 连接断开: {}", remote);
        // 从注册表中清除该连接
        channelRegistry.unregister(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf frame) {
        // 入站完整帧 hex dump
        if (log.isInfoEnabled()) {
            log.info("[HANDLER←DECODER] 完整帧 {} bytes:\n{}",
                    frame.readableBytes(), ByteBufUtil.prettyHexDump(frame));
        }

        try {
            handleFrame(frame, ctx);
        } catch (Exception e) {
            log.error("处理集中器报文异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("集中器通道异常: {}", cause.getMessage());
        ctx.close();
    }

    // ──────── 帧解析与分发 ────────

    /**
     * 解析一帧完整数据。
     * <p>
     * 帧结构（从 0x68 起）：
     * <pre>
     * [0x68][区域(1)][地址(5 BCD)][功能(1)][长度(2 LE)][数据(N)][ReplyFlag(1)][SER(1)][CS(1)][0x16]
     * </pre>
     */
    private void handleFrame(ByteBuf frame, ChannelHandlerContext ctx) {
        int readable = frame.readableBytes();
        if (readable < 11) {
            log.debug("帧过短 ({} 字节)，忽略", readable);
            return;
        }

        // 区域识别码
        int regionCode = frame.getByte(OFF_REGION) & 0xFF;

        // 5 字节 BCD 集中器地址
        byte[] addr = new byte[5];
        frame.getBytes(OFF_ADDR, addr, 0, 5);
        String collectorAddr = BcdUtils.bcdToString(addr, 0, 5);

        // 功能码
        byte func = frame.getByte(OFF_FUNC);

        // 数据长度
        int dataLenLo = frame.getByte(OFF_DATA_LEN) & 0xFF;
        int dataLenHi = frame.getByte(OFF_DATA_LEN + 1) & 0xFF;
        int dataLen = dataLenLo | (dataLenHi << 8);

        // 回复标志 + SER
        int replyFlagOff = OFF_DATA + dataLen;
        int serOff = replyFlagOff + 1;
        byte replyFlag = readable > replyFlagOff ? frame.getByte(replyFlagOff) : 0;
        byte ser = readable > serOff ? frame.getByte(serOff) : 0;

        // ── 升级为 INFO 级别联调日志 ──
        log.info("[集中器帧] 地址={} 区域=0x{} 功能=0x{} 数据长度={} 回复标志=0x{} 序列号={}",
                collectorAddr,
                Integer.toHexString(regionCode),
                Integer.toHexString(func & 0xFF),
                dataLen,
                Integer.toHexString(replyFlag & 0xFF),
                ser);

        // 功能码分发
        switch (func) {
            case FUNC_READ_ALL_CURRENT:      // 0x15：读取所有表计当前数据
            case FUNC_READ_SINGLE_REALTIME:  // 0x44：读取单表实时数据
                handleMeterData(frame, dataLen, collectorAddr, ctx);
                break;
            default:
                log.info("[集中器帧] 未处理的功能码 0x{} 来自集中器 addr={}",
                        Integer.toHexString(func & 0xFF), collectorAddr);
                break;
        }
    }

    // ──────── 表计数据处理 ────────

    /**
     * 从数据域中逐条解析 CJ/T 188 表计记录并入库。
     * <p>
     * 数据库写入操作提交至独立业务线程池 {@link #businessExecutor}，
     * 确保不阻塞 Netty Worker I/O 线程。
     */
    private void handleMeterData(ByteBuf frame, int dataLen, String collectorAddr,
                                  ChannelHandlerContext ctx) {
        if (dataLen < MeterRecordParser.RECORD_LEN_188) {
            log.info("[集中器帧] 数据域过短 (len={})，非 CJ/T 188 表计数据", dataLen);
            return;
        }

        // 提取数据域原始字节
        byte[] data = new byte[dataLen];
        frame.getBytes(OFF_DATA, data, 0, dataLen);

        log.info("[集中器帧] 数据域 hexDump ({} bytes):\n{}",
                dataLen, ByteBufUtil.hexDump(data));

        long nowTs = System.currentTimeMillis();
        int recordCount = 0;
        int offset = 0;

        while (offset + MeterRecordParser.RECORD_LEN_188 <= dataLen) {
            MeterRecordParser.MeterRecord record =
                    MeterRecordParser.parse188Record(data, offset);
            offset += MeterRecordParser.RECORD_LEN_188;

            if (record == null) continue;

            // ── 解析结果日志（INFO 级别，联调可见） ──
            log.info("[表计记录] 表类型=0x{} 表地址={} 厂商={} 累计流量={} L ({} m³)",
                    Integer.toHexString(record.getMeterType() & 0xFF),
                    record.getMeterAddr(),
                    record.getVendorCode(),
                    record.getCumulativeFlow().toPlainString(),
                    record.getCumulativeFlow().divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP));

            // 根据表计钢印号匹配本地 t_meter
            Meter meter = findByStealNo(record.getMeterAddr());
            if (meter == null) {
                log.info("[表计记录] 未匹配到本地表计 stealNo={}，跳过入库", record.getMeterAddr());
                continue;
            }

            // 将表计 stealNo 注册到 ChannelRegistry（供阀控指令查找通道）
            channelRegistry.register(record.getMeterAddr(), ctx);

            log.info("[表计记录] 匹配到本地表计 meterId={} name={} customerId={}",
                    meter.getId(), meter.getName(), meter.getCustomerId());

            // 构造入库实体
            MeterReading r = new MeterReading();
            r.setMeterId(meter.getId());
            r.setStealNo(record.getMeterAddr());
            r.setTimestamp(LocalDateTime.now());
            r.setReadVal(record.getCumulativeFlow());
            r.setKwh(record.getCumulativeFlow()
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP));
            r.setVender("积成");
            r.setCustomerCode(collectorAddr);
            r.setCreateTime(LocalDateTime.now());
            r.setUpdateTime(LocalDateTime.now());

            // ── 异步提交到业务线程池，不阻塞 Netty I/O 线程 ──
            final MeterReading reading = r;
            final int idx = recordCount;
            businessExecutor.submit(() -> {
                try {
                    meterReadingMapper.insert(reading);
                    log.info("[表计记录] 入库成功 meterId={} stealNo={} 读数={} L",
                            meter.getId(), record.getMeterAddr(),
                            record.getCumulativeFlow().toPlainString());
                } catch (Exception e) {
                    log.error("[表计记录] 入库失败 meterId={} stealNo={}: {}",
                            meter.getId(), record.getMeterAddr(), e.getMessage());
                }
            });

            recordCount++;
        }

        if (recordCount > 0) {
            log.info("[集中器帧] 集中器 {} 解析出 {} 条表计记录，已提交异步入库", collectorAddr, recordCount);
        }
    }

    // ──────── 辅助方法 ────────

    /**
     * 根据表计钢印号匹配本地表计记录。
     */
    private Meter findByStealNo(String stealNo) {
        if (stealNo == null || stealNo.isBlank() || "0".equals(stealNo)) return null;
        return meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>().eq(Meter::getStealNo, stealNo));
    }
}
