package com.competition.energycontrollerplatform.netty;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty Channel 注册表。
 * <p>
 * 维护集中器地址（5 字节 BCD 解码后的 stealNo 字符串）
 * → {@link ChannelHandlerContext} 的映射关系，
 * 供 {@code JichengValveControlHook} 下发阀控指令时查找对应的 TCP 通道。
 * <p>
 * <b>线程安全：</b>使用 {@link ConcurrentHashMap}，支持 Netty I/O 线程写入、
 * 业务线程读取的并发场景。
 */
@Component
public class ChannelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChannelRegistry.class);

    /** stealNo → ChannelHandlerContext */
    private final ConcurrentHashMap<String, ChannelHandlerContext> channels = new ConcurrentHashMap<>();

    /**
     * 注册集中器通道。
     *
     * @param stealNo 集中器下挂表计的钢印号（作为集中器的逻辑标识）
     * @param ctx     Netty ChannelHandlerContext
     */
    public void register(String stealNo, ChannelHandlerContext ctx) {
        if (stealNo == null || stealNo.isBlank()) return;
        String oldAddr = ctx.channel().remoteAddress() != null
                ? ctx.channel().remoteAddress().toString() : "unknown";
        ChannelHandlerContext prev = channels.put(stealNo, ctx);
        if (prev != null && prev != ctx) {
            log.info("[ChannelRegistry] 集中器通道已更新 stealNo={} oldRemote={} newRemote={}",
                    stealNo, oldAddr, oldAddr);
        } else {
            log.info("[ChannelRegistry] 集中器通道已注册 stealNo={} remote={} 当前在线={}",
                    stealNo, oldAddr, channels.size());
        }
    }

    /**
     * 注销集中器通道（连接断开时调用）。
     *
     * @param ctx Netty ChannelHandlerContext
     */
    public void unregister(ChannelHandlerContext ctx) {
        channels.values().removeIf(existingCtx -> {
            if (existingCtx == ctx) {
                String remote = ctx.channel().remoteAddress() != null
                        ? ctx.channel().remoteAddress().toString() : "unknown";
                log.info("[ChannelRegistry] 集中器通道已注销 remote={} 当前在线={}",
                        remote, channels.size());
                return true;
            }
            return false;
        });
    }

    /**
     * 根据 stealNo 查找对应的 Netty ChannelHandlerContext。
     *
     * @param stealNo 表计钢印号
     * @return ChannelHandlerContext，未找到返回 {@code null}
     */
    public ChannelHandlerContext getChannel(String stealNo) {
        if (stealNo == null || stealNo.isBlank()) return null;
        return channels.get(stealNo);
    }

    /** 当前在线的集中器通道数量 */
    public int onlineCount() {
        return channels.size();
    }
}
