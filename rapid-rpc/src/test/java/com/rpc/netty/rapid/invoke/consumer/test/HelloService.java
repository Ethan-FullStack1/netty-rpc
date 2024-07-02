package com.rpc.netty.rapid.invoke.consumer.test;

/**
 * 测试接口
 *
 * @author debao.yang
 * @since 2024/7/2 19:43
 */
public interface HelloService {

    String hello(String name);

    String hello(User user);

}
