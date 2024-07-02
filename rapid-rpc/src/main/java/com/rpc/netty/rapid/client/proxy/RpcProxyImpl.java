package com.rpc.netty.rapid.client.proxy;

import com.rpc.netty.rapid.client.RpcClientHandler;
import com.rpc.netty.rapid.client.RpcConnetManager;
import com.rpc.netty.rapid.client.RpcFuture;
import com.rpc.netty.rapid.codec.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author debao.yang
 * @since 2024/7/2 18:43
 */
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

    private Class<T> clazz;
    private long timeout;
    private RpcConnetManager rpcConnetManager;

    public RpcProxyImpl(RpcConnetManager rpcConnetManager,
                        Class<T> interfaceClass,
                        long timeout) {
        this.clazz = interfaceClass;
        this.timeout = timeout;
        this.rpcConnetManager = rpcConnetManager;
    }

    @Override
    public Object invoke(Object proxy,
                         Method method,
                         Object[] args) throws Throwable {
        // 1、设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        // 2、选择一个合适的Client处理器
        RpcClientHandler handler = rpcConnetManager
                .chooseHandler();
        // 3、发送一个客户端请求，返回结果
        RpcFuture future = handler.sendRequest(request);
        return future.get(timeout, TimeUnit.SECONDS);
    }

    /**
     * 异步的代理接口实现，真正的抛出去RpcFuture 给业务方做实际的回调等待处理
     *
     * @param funcName 要调用的方法名称
     * @param args     方法执行的参数
     * @return com.rpc.netty.rapid.client.RpcFuture
     * @author debao.yang
     * @since 2024/7/2 19:31
     */
    @Override
    public RpcFuture call(String funcName, Object... args) {
        // 设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.clazz.getName());
        request.setMethodName(funcName);
        request.setParameters(args);

        // todo: 对应的方法参数类型应该通过 类类型 + 方法名称 通过反射得到parameterTypes
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        // 选择一个合适的Client任务处理器
        RpcClientHandler handler = rpcConnetManager
                .chooseHandler();

        return handler.sendRequest(request);
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }
        return classType;
    }
}
