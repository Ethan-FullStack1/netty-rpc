package com.rpc.netty.rapid.invoke.provider.test;

import com.rpc.netty.rapid.config.provider.ProviderConfig;
import com.rpc.netty.rapid.config.provider.RpcServerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author debao.yang
 * @since 2024/7/2 19:46
 */
public class ProviderStarter {

    public static void main(String[] args) {
        // 服务端启动
        new Thread(() -> {
            try {
                // 每一个具体的服务提供者的配置类
                ProviderConfig providerConfig = new ProviderConfig();
                providerConfig.setInterface("com.rpc.netty.rapid.invoke.consumer.test.HelloService");
                HelloServiceImpl service = HelloServiceImpl.class.newInstance();
                providerConfig.setRef(service);

                // 把所有的ProviderConfig添加到集合中
                List<ProviderConfig> providerConfigs = new ArrayList<>();
                providerConfigs.add(providerConfig);

                RpcServerConfig config = new RpcServerConfig(providerConfigs);
                config.setPort(8765);
                config.exporter();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
