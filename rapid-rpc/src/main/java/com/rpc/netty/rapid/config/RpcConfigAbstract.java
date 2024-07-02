package com.rpc.netty.rapid.config;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author debao.yang
 * @since 2024/7/2 10:53
 */
public abstract class RpcConfigAbstract {

    private final AtomicInteger generator = new AtomicInteger(0);

    protected String id;

    protected String interfaceClass = null;

    // 服务的调用方(consumer端特有的属性)
    protected Class<?> proxyClass = null;

    public String getId() {
        if (StringUtils.isBlank(id)) {
            id = "rapid-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInterface(String interfaceClass){
        this.interfaceClass = interfaceClass;
    }

    public String getInterface(){
        return this.interfaceClass;
    }
}
