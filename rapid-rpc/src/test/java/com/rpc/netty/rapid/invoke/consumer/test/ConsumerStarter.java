package com.rpc.netty.rapid.invoke.consumer.test;

import com.rpc.netty.rapid.client.RpcClient;
import com.rpc.netty.rapid.client.RpcFuture;
import com.rpc.netty.rapid.client.proxy.RpcAsyncProxy;

import java.util.concurrent.ExecutionException;

/**
 * 调用方
 *
 * @author debao.yang
 * @since 2024/7/2 19:57
 */
public class ConsumerStarter {

    public static void sync() {
        // rpcClient
        RpcClient rpcClient = new RpcClient();
        rpcClient.initClient("127.0.0.1:8765", 3000);
        HelloService service = rpcClient.invokeSync(HelloService.class);
        String result = service.hello("zhang3");
        System.err.println(result);
    }

    public static void async() throws InterruptedException, ExecutionException {
        RpcClient rpcClient = new RpcClient();
        rpcClient.initClient("127.0.0.1:8765",3000);
        RpcAsyncProxy proxy = rpcClient.invokeAsync(HelloService.class);
        RpcFuture future = proxy.call("hello", "li4");
        RpcFuture future1 = proxy.call("hello", new User("001", "wang5"));


        Object result = future.get();
        Object result2 = future1.get();
        System.err.println("result: " + result);
        System.err.println("result2: " + result2);
    }

    public static void main(String[] args) throws Exception{
        sync();
    }

}
