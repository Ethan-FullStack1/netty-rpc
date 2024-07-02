package com.rpc.netty.rapid.config.provider;

import com.rpc.netty.rapid.config.RpcConfigAbstract;

/**
 * 接口名称
 * 程序对象
 *
 * @author debao.yang
 * @since 2024/7/2 09:09
 */
public class ProviderConfig extends RpcConfigAbstract {

    protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
