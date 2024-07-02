package com.rpc.netty.rapid.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * Rpc接口的返回定义
 *
 * @author debao.yang
 * @since 2024/7/2 07:56
 */
@Data
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = -4953814251407762400L;

    private String requestId;

    private Object result;

    private Throwable throwable;

}
