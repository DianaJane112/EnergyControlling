package com.competition.energycontrollerplatform.integration.tcp.server;

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
 * 积成水表 TCP 网关（旧版，已被 {@link com.competition.energycontrollerplatform.netty.NettyServer} 取代）。
 * 启动后监听指定端口，接收集中器 GPRS 长连接报文。
 *
 * @deprecated 请使用 {@link com.competition.energycontrollerplatform.netty.NettyServer}
 */
// @Component — 已由 netty.NettyServer 接管，禁用避免端口冲突
@Deprecated
public class JiChengTcpServer {

    private static final Logger log = LoggerFactory.getLogger(JiChengTcpServer.class);

    @Value("${integration.jicheng.tcp-port:18080}")
    private int port;

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public JiChengTcpServer(MeterMapper meterMapper,
                            MeterReadingMapper meterReadingMapper) {
        this.meterMapper = meterMapper;
        this.meterReadingMapper = meterReadingMapper;
    }

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(4);

        Thread serverThread = new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(port))
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                String remote = ch.remoteAddress().toString();
                                ch.pipeline().addLast(
                                        new JiChengChannelHandler(
                                                meterMapper, meterReadingMapper, remote));
                            }
                        });

                ChannelFuture future = bootstrap.bind().sync();
                log.info("=== 积成水表 TCP 网关已启动，监听端口 {} ===", port);
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("积成 TCP 服务器被中断");
            } catch (Exception e) {
                log.error("积成 TCP 服务器启动失败", e);
            } finally {
                shutdown();
            }
        }, "jicheng-tcp-boss");

        serverThread.setDaemon(false);
        serverThread.start();
        log.info("积成 TCP 服务器线程已启动");
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭积成 TCP 服务器...");
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("积成 TCP 服务器已关闭");
    }
}
