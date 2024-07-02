package com.rpc.netty.rapid.client;

import com.rpc.netty.rapid.codec.RpcRequest;
import com.rpc.netty.rapid.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.SneakyThrows;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实际的业务处理器
 *
 * @author debao.yang
 * @since 2024/7/2 06:03
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Getter
    private Channel channel;
    @Getter
    private SocketAddress remotePeer;
    private final Map<String/* requestId */, RpcFuture> pendingRpcTable =
            new ConcurrentHashMap<>();

    /**
     * 通道注册的时候触发此方法
     *
     * @param ctx ctx
     * @author debao.yang
     * @since 2024/7/2 06:34
     */
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    /**
     * 当通道激活的时候触发此方法
     *
     * @param ctx 通道处理器上下文
     * @author debao.yang
     * @since 2024/7/2 06:31
     */
    @SneakyThrows
    public void channelActive(ChannelHandlerContext ctx) {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        RpcFuture future = pendingRpcTable.get(requestId);
        if (future != null) {
            pendingRpcTable.remove(requestId);
            future.done(rpcResponse);
        }

    }

    /**
     * Netty提供了一种主动关闭连接的方法，发送一个Unpooled.EMPTY_BUFFER
     * 这样ChannelFutureListener的close事件就会监听到并关闭连接
     *
     * @author debao.yang
     * @since 2024/7/2 06:42
     */
    public void close() {

        channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 异步发送请求方法
     *
     * @param rpcRequest 请求的参数
     * @return com.rpc.netty.rapid.client.RpcFuture
     * @author debao.yang
     * @since 2024/7/2 12:18
     */
    public RpcFuture sendRequest(RpcRequest rpcRequest) {
        RpcFuture future = new RpcFuture(rpcRequest);
        pendingRpcTable.put(rpcRequest.getRequestId(), future);
        channel.writeAndFlush(rpcRequest);
        return future;
    }
}
