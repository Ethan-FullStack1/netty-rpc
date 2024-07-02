package com.rpc.netty.rapid.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * Rpc接口的请求定义
 *
 * @author debao.yang
 * @since 2024/7/2 07:56
 */
@Data
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 4819719995530933531L;

    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;
}
