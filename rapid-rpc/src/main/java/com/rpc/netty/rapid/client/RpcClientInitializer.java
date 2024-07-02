package com.rpc.netty.rapid.client;

import com.rpc.netty.rapid.codec.RpcDecoder;
import com.rpc.netty.rapid.codec.RpcEncoder;
import com.rpc.netty.rapid.codec.RpcRequest;
import com.rpc.netty.rapid.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author debao.yang
 * @since 2024/7/2 06:14
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline cp = ch.pipeline();
        // 编解码的handler
        cp.addLast(new RpcEncoder(RpcRequest.class));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class));
        // 实际的业务处理器 RpcClientHandler
        cp.addLast(new RpcClientHandler());


    }
}
