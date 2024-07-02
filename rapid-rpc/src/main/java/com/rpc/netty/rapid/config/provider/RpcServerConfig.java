package com.rpc.netty.rapid.config.provider;

import com.rpc.netty.rapid.server.RpcServer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 服务器端启动配置类
 *
 * @author debao.yang
 * @since 2024/7/2 19:33
 */
@Slf4j
public class RpcServerConfig {

    private final String host = "127.0.0.1";

    protected int port;

    @Getter
    @Setter
    private List<ProviderConfig> providerConfigs;

    private RpcServer rpcServer = null;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public void exporter() {
        if (rpcServer == null) {
            try {
                rpcServer = new RpcServer(host + ":" + port);
            } catch (Exception e) {
                log.error("RpcServerConfig exporter exception: ", e);
            }

            for (ProviderConfig pc : providerConfigs) {
                rpcServer.registerProcessor(pc);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
