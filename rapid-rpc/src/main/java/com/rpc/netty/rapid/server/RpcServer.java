package com.rpc.netty.rapid.server;

import com.rpc.netty.rapid.codec.RpcDecoder;
import com.rpc.netty.rapid.codec.RpcEncoder;
import com.rpc.netty.rapid.codec.RpcRequest;
import com.rpc.netty.rapid.codec.RpcResponse;
import com.rpc.netty.rapid.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 服务端
 *
 * @author debao.yang
 * @since 2024/7/2 08:47
 */
@Slf4j
public class RpcServer {

    private String serverAddress;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workGroup = new NioEventLoopGroup();

    private volatile Map<String,/* interface name */Object> handlerMap =
            new HashMap<>();

    public RpcServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    private void start() throws InterruptedException {
        ServerBootstrap sb = new ServerBootstrap();
        sb.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4
                                , 0, 0));
                        cp.addLast(new RpcDecoder(RpcRequest.class));
                        cp.addLast(new RpcEncoder(RpcResponse.class));
                        cp.addLast(new RpcServerHandler(handlerMap));
                    }
                });
        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);

        ChannelFuture channelFuture = sb.bind(host, port).sync();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("server success binding to {}", serverAddress);
            } else {
                log.info("server fail binding to {}", serverAddress);
                throw new Exception("server start fail, cause: " + future.cause());
            }
        });

        try {
            channelFuture.await(5, TimeUnit.SECONDS);
            if (channelFuture.isSuccess()) {
                log.info("start rapid rpc success!");
            }
        } catch (InterruptedException e) {
            log.error("start rapid rpc occur interrupted, ex: +", e);
        }
    }

    /**
     * 程序注册器
     *
     * @author debao.yang
     * @since 2024/7/2 09:07
     */
    public void registerProcessor(ProviderConfig providerConfig) {
        // key: providerConfig.interface (userService接口权限命名)
        // value: providerConfig.ref (userService接口下的具体实现类 userServiceImpl
        // 实例对象)
        handlerMap.put(providerConfig.getInterface(), providerConfig.getRef());
    }

    public void close() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();

    }

}
