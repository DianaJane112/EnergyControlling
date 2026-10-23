package com.competition.energycontrollerplatform.netty;

import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * 积成协议 Netty TCP 通信网关。
 * <p>
 * 在 Spring Boot 启动时异步监听指定端口（默认 18080），
 * 接收集中器 GPRS 长连接报文，经断帧器、协议处理器完成解码与持久化。
 * <p>
 * Pipeline 结构：
 * <pre>
 * SocketChannel
 *   → {@link JiChengFrameDecoder}      (ByteToMessageDecoder，断帧)
 *   → {@link JiChengProtocolHandler}   (SimpleChannelInboundHandler，解析入库)
 * </pre>
 */
@Component
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Value("${integration.jicheng.tcp-port:18080}")
    private int port;

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final ChannelRegistry channelRegistry;

    /** 主 Reactor 线程组（接受连接） */
    private EventLoopGroup bossGroup;
    /** 从 Reactor 线程组（处理 I/O） */
    private EventLoopGroup workerGroup;

    public NettyServer(MeterMapper meterMapper, MeterReadingMapper meterReadingMapper,
                       ChannelRegistry channelRegistry) {
        this.meterMapper = meterMapper;
        this.meterReadingMapper = meterReadingMapper;
        this.channelRegistry = channelRegistry;
    }

    /**
     * Spring Boot 启动后异步启动 Netty TCP 服务。
     * 使用独立守护线程避免阻塞主线程。
     */
    @PostConstruct
    public void start() {
        // BossGroup：1 个线程足够，仅负责接收连接
        bossGroup = new NioEventLoopGroup(1);
        // WorkerGroup：4 个线程处理 I/O 读写与业务解码
        workerGroup = new NioEventLoopGroup(4);

        Thread serverThread = new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(port))
                        // 半连接队列长度
                        .option(ChannelOption.SO_BACKLOG, 128)
                        // 长连接保活
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        // 禁用 Nagle 算法，低延迟
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                // Pipeline：断帧器 → 协议处理器
                                ch.pipeline().addLast(
                                        new JiChengFrameDecoder(),
                                        new JiChengProtocolHandler(meterMapper, meterReadingMapper, channelRegistry));
                            }
                        });

                ChannelFuture future = bootstrap.bind().sync();
                log.info("=== 积成水表 Netty TCP 网关已启动，监听端口 {} ===", port);
                // 阻塞直到 Channel 关闭
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("积成 TCP 服务器线程被中断");
            } catch (Exception e) {
                log.error("积成 TCP 服务器启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-jicheng-boss");

        serverThread.setDaemon(false);
        serverThread.start();
        log.info("积成 TCP 服务器异步启动线程已提交");
    }

    /**
     * 应用关闭时优雅终止 EventLoopGroup。
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭积成 TCP 服务器...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("积成 TCP 服务器已关闭");
    }
}
