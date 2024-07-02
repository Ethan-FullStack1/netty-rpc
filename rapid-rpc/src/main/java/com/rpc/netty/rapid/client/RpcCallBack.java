package com.rpc.netty.rapid.client;

/**
 * @author debao.yang
 * @since 2024/7/2 12:21
 */
public interface RpcCallBack {

    void success(Object result);

    void failure(Throwable cause);

}
