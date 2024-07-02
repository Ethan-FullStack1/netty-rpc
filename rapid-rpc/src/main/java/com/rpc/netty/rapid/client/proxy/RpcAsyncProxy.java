package com.rpc.netty.rapid.client.proxy;

import com.rpc.netty.rapid.client.RpcFuture;

/**
 * 异步代理接口
 *
 * @author debao.yang
 * @since 2024/7/2 19:06
 */
public interface RpcAsyncProxy {

    RpcFuture call(String funcName, Object... args);

}
