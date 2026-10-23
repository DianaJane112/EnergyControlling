package com.competition.energycontrollerplatform.integration.hook;

import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.ValveControl;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.ValveControlMapper;
import com.competition.energycontrollerplatform.netty.ChannelRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 积成水表阀控 Hook（TCP Netty 通道）。
 * <p>
 * 通过 Netty TCP 长连接向积成集中器下发阀控指令帧。
 * 当前版本中，积成 TCP 阀控帧的具体功能码和帧格式需依据积成 iES-CMR 通信协议文档确认，
 * 此处提供框架代码和默认帧模板，待协议文档到位后补充精确的帧编码逻辑。
 * <p>
 * <b>当前状态：</b>如 ChannelRegistry 中无对应的集中器通道（集中器未上线），
 * 仅写入 {@code t_valve_control} 审计记录并将 task_status 标记为 "NO_CHANNEL"。
 */
@Component
@RequiredArgsConstructor
public class JichengValveControlHook {

    private static final Logger log = LoggerFactory.getLogger(JichengValveControlHook.class);

    /**
     * 阀控指令功能码（待积成协议文档确认）。
     * 当前使用 0x55 作为占位值。
     */
    private static final byte FUNC_VALVE_CONTROL = 0x55;

    /** 开阀 */
    private static final byte VALVE_OPEN = 0x01;
    /** 关阀 */
    private static final byte VALVE_CLOSE = 0x02;

    private final ChannelRegistry channelRegistry;
    private final MeterMapper meterMapper;
    private final ValveControlMapper valveControlMapper;

    /**
     * 下发开阀指令。
     *
     * @param meterId  表计ID
     * @param stealNo  表钢印号
     * @param operator 操作人
     * @param reason   操作原因
     */
    public void openValve(Long meterId, String stealNo, String operator, String reason) {
        sendValveFrame(meterId, stealNo, operator, reason, VALVE_OPEN, "OPEN");
    }

    /**
     * 下发关阀指令。
     *
     * @param meterId  表计ID
     * @param stealNo  表钢印号
     * @param operator 操作人
     * @param reason   操作原因
     */
    public void closeValve(Long meterId, String stealNo, String operator, String reason) {
        sendValveFrame(meterId, stealNo, operator, reason, VALVE_CLOSE, "CLOSE");
    }

    // ──────── 私有方法 ────────

    private void sendValveFrame(Long meterId, String stealNo, String operator,
                                 String reason, byte valveAction, String actionType) {
        if (stealNo == null || stealNo.isBlank()) {
            log.warn("[积成阀控] stealNo 为空，跳过 meterId={}", meterId);
            return;
        }

        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.warn("[积成阀控] 表计不存在 meterId={}", meterId);
            return;
        }

        // 查找对应的 Netty 通道
        ChannelHandlerContext ctx = channelRegistry.getChannel(stealNo);
        String taskStatus;
        String msg;

        if (ctx != null && ctx.channel().isActive()) {
            try {
                // 构造阀控指令帧
                ByteBuf frame = buildValveFrame(stealNo, valveAction);
                ctx.writeAndFlush(frame);
                taskStatus = "PENDING";
                msg = "TCP 阀控指令已下发，等待集中器确认";
                log.info("[积成阀控] TCP 指令已下发 meterId={} stealNo={} action={}",
                        meterId, stealNo, actionType);
            } catch (Exception e) {
                log.error("[积成阀控] TCP 指令发送异常 meterId={} stealNo={}: {}",
                        meterId, stealNo, e.getMessage());
                taskStatus = "SEND_FAILED";
                msg = "TCP 发送异常：" + e.getMessage();
            }
        } else {
            log.warn("[积成阀控] 集中器通道不存在或已断开 stealNo={} meterId={}",
                    stealNo, meterId);
            taskStatus = "NO_CHANNEL";
            msg = "集中器离线或通道未注册";
        }

        // 写入阀控审计记录
        ValveControl vc = new ValveControl();
        vc.setMeterId(meterId);
        vc.setStealNo(stealNo);
        vc.setVender(meter.getVender());
        vc.setSendbody("FUNC=" + String.format("0x%02X", FUNC_VALVE_CONTROL)
                + " ACTION=" + actionType);
        vc.setCtrlCode1((int) valveAction);
        vc.setCtrlCode2(0);
        vc.setBatchid(stealNo + "_" + System.currentTimeMillis());
        vc.setTaskStatus(taskStatus);
        vc.setType("JC_TCP");
        vc.setOperator(operator);
        vc.setOperateDate(LocalDateTime.now());
        vc.setMsg(msg);
        vc.setCreateTime(LocalDateTime.now());
        vc.setUpdateTime(LocalDateTime.now());
        vc.setCreateBy(operator);
        valveControlMapper.insert(vc);
    }

    /**
     * 构造积成阀控 TCP 指令帧。
     * <p>
     * 帧格式（待积成协议文档确认）：
     * <pre>
     * [0xFE][0xFE][0x68][Region(1)][Addr(5 BCD)][Func(1)][DataLen(2 LE)][Data...][CS(1)][0x16]
     * </pre>
     * 当前使用默认帧模板，待协议文档到位后补充精确编码逻辑。
     *
     * @param stealNo     表钢印号（将编码为 5 字节 BCD 地址）
     * @param valveAction 阀控动作：0x01=开阀, 0x02=关阀
     * @return 待发送的 ByteBuf 帧
     */
    private ByteBuf buildValveFrame(String stealNo, byte valveAction) {
        // 将 stealNo 字符串编码为 5 字节 BCD
        byte[] bcdAddr = encodeBcd5(stealNo);

        // 帧结构：引导码(2) + 0x68 + 区域(1) + 地址(5) + 功能(1) + 长度(2 LE) + 数据(1) + CS(1) + 0x16
        int frameLen = 2 + 1 + 1 + 5 + 1 + 2 + 1 + 1 + 1; // = 15 字节
        ByteBuf buf = Unpooled.buffer(frameLen);

        // 引导码
        buf.writeByte(0xFE);
        buf.writeByte(0xFE);
        // 帧头
        buf.writeByte(0x68);
        // 区域码（GPRS 模式固定 0xEE）
        buf.writeByte((byte) 0xEE);
        // 5 字节 BCD 地址
        buf.writeBytes(bcdAddr);
        // 功能码（阀控）
        buf.writeByte(FUNC_VALVE_CONTROL);
        // 数据长度（1 字节数据，小端序）
        buf.writeShortLE(1);
        // 数据域：阀控动作
        buf.writeByte(valveAction);
        // CS 校验和（从 0x68 到数据域末尾累加）
        int sum = 0x68 + 0xEE;
        for (byte b : bcdAddr) sum += (b & 0xFF);
        sum += (FUNC_VALVE_CONTROL & 0xFF);
        sum += 1; // 数据长度低字节
        sum += 0; // 数据长度高字节
        sum += (valveAction & 0xFF);
        buf.writeByte((byte) (sum & 0xFF));
        // 帧尾
        buf.writeByte(0x16);

        return buf;
    }

    /**
     * 将 stealNo 数字字符串编码为 5 字节 BCD。
     * <p>左侧补零到 10 位十进制数字，每字节存储 2 位。</p>
     */
    private byte[] encodeBcd5(String stealNo) {
        byte[] bcd = new byte[5];
        // 左侧补零到10位
        String padded = String.format("%010d",
                Long.parseLong(stealNo.replaceAll("[^0-9]", "0")));
        for (int i = 0; i < 5; i++) {
            int high = padded.charAt(i * 2) - '0';
            int low = padded.charAt(i * 2 + 1) - '0';
            bcd[i] = (byte) ((high << 4) | low);
        }
        return bcd;
    }
}
