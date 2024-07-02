package com.rpc.netty.rapid.invoke.provider.test;

import com.rpc.netty.rapid.invoke.consumer.test.HelloService;
import com.rpc.netty.rapid.invoke.consumer.test.User;

/**
 * 接口实际的实现
 *
 * @author debao.yang
 * @since 2024/7/2 19:45
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello!" + name;
    }

    @Override
    public String hello(User user) {
        return "hello!" + user.getName();
    }
}
