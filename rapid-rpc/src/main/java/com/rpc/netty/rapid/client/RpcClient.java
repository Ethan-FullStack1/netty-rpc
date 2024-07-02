package com.rpc.netty.rapid.client;

import com.rpc.netty.rapid.client.proxy.RpcAsyncProxy;
import com.rpc.netty.rapid.client.proxy.RpcProxyImpl;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端的类
 *
 * @author debao.yang
 * @since 2024/7/2 07:48
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;

    private RpcConnetManager connetManager;

    /* 异步调用的代理类缓存 */
    private final Map<Class<?>, Object> syncProxyInstanceMap =
            new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> asyncProxyIntanceMap =
            new ConcurrentHashMap<>();

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        this.connetManager = new RpcConnetManager();
        connect();
    }

    private void connect() {
        this.connetManager.connect(serverAddress);
    }

    public void stop() {
        this.connetManager.stop();
    }

    /**
     * 同步调用方法，走的是JDK的代理
     *
     * @param interfaceClass 要调用的方法
     * @return T
     * @author debao.yang
     * @since 2024/7/2 18:50
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeSync(Class<T> interfaceClass) {

        if (syncProxyInstanceMap.containsKey(interfaceClass)) {
            return ((T) syncProxyInstanceMap.get(interfaceClass));
        } else {
            Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                    new Class<?>[]{interfaceClass},
                    new RpcProxyImpl<>(connetManager, interfaceClass, timeout));
            syncProxyInstanceMap.put(interfaceClass, proxy);
            return ((T) proxy);
        }
    }

    /**
     * 异步调用方式的方法
     *
     * @param interfaceClass 要实现的代理接口
     */
    public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
        if (asyncProxyIntanceMap.containsKey(interfaceClass)) {
            return (RpcAsyncProxy) asyncProxyIntanceMap.get(interfaceClass);
        } else {
            RpcProxyImpl<T> asyncProxyInstance =
                    new RpcProxyImpl<>(connetManager, interfaceClass,
                            timeout);
            asyncProxyIntanceMap.put(interfaceClass, asyncProxyInstance);
            return asyncProxyInstance;
        }
    }
}
