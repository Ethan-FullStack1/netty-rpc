package com.rpc.netty.rapid.client;

/**
 * 客户端的类
 *
 * @author debao.yang
 * @since 2024/7/2 07:48
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnetManager.getInstance().connect(serverAddress);
    }

    public void stop() {
        RpcConnetManager.getInstance().stop();
    }

}
