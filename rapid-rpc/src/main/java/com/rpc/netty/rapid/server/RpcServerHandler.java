package com.rpc.netty.rapid.server;

import com.rpc.netty.rapid.codec.RpcRequest;
import com.rpc.netty.rapid.codec.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author debao.yang
 * @since 2024/7/2 08:56
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String, Object> handlerMap;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16,
            600L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                RpcRequest rpcRequest) throws Exception {

        // 1、解析rpcRequest
        // 2、从handlerMap中找到具体的接口(key)所绑定的实例(bean)
        // 3、通过反射cglib调用 具体方法传递相相关执行参数执行逻辑即可
        // 4、返回响应信息给调用方

        executor.submit(() -> {
            RpcResponse response = new RpcResponse();
            response.setRequestId(rpcRequest.getRequestId());
            try {
                Object result = handle(rpcRequest);
                response.setResult(result);
            } catch (Throwable t) {
                response.setThrowable(t);
                log.error("rpc service handle request Throwable: " + t);
            }

            ctx.writeAndFlush(response)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            // afterRpcHook
                        }
                    });
        });
    }

    /**
     * 解析Request请求并且去通过反射，获取具体的本地服务实例后执行具体的方法
     *
     * @param rpcRequest rpc请求的参数
     * @return java.lang.Object
     * @author debao.yang
     * @since 2024/7/2 11:56
     */
    private Object handle(RpcRequest rpcRequest) throws InvocationTargetException {

        String className = rpcRequest.getClassName();
        Object serviceRef = handlerMap.get(className);
        Class<?> serviceClass = serviceRef.getClass();

        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Objects[] parameters = rpcRequest.getParameters();

        // JDK reflect

        // Cglib
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);

        return serviceFastMethod.invoke(serviceRef, parameters);
    }

    /**
     * 异常处理关闭连接
     *
     * @param ctx   ctx
     * @param cause 异常
     * @author debao.yang
     * @since 2024/7/2 11:19
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) throws Exception {
        log.error("server caught exception" + cause);
        ctx.close();
    }

}
