package com.rpc.netty.rapid.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC通讯的管理器
 *
 * @author debao.yang
 * @since 2024/7/2 05:43
 */
@Slf4j
public class RpcConnetManager {

    // private static volatile RpcConnetManager RPC_CONNET_MANAGER =
    //         new RpcConnetManager();

    public RpcConnetManager() {
    }

    /**
     * 一个连接的地址对应一个实际的业务处理器(client)
     */
    private final Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap =
            new ConcurrentHashMap<>();

    /**
     * 所有连接成功的任务执行器列表
     */
    private final CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList =
            new CopyOnWriteArrayList<>();

    /**
     * 用于异步提交链接请求的线程池
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 1,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(65536));

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private final Lock connectedLock = new ReentrantLock();

    private final Condition connectedCondition = connectedLock.newCondition();

    private final long connectTimeoutMills = 6000;
    private volatile boolean isRunning = true;

    private volatile AtomicInteger handlerIdx = new AtomicInteger(0);


    // 1、异步链接 线程池 真正的发起链接，连接失败监听，链接成功监听
    // 2、对于连接进来的资源做一个缓存(做一个管理) updateConnectedServer

    /**
     * 发起链接方法
     *
     * @param serverAddress 要连接的地址
     * @author debao.yang
     * @since 2024/7/2 05:52
     */
    public void connect(final String serverAddress) {
        List<String> allServerAdress = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(allServerAdress);
    }

    /**
     * 更新缓存信息，并异步发起链接
     *
     * @param allServerAdress 所有的服务地址
     * @author debao.yang
     * @since 2024/7/2 05:52
     */
    public void updateConnectedServer(List<String> allServerAdress) {
        if (CollectionUtils.isNotEmpty(allServerAdress)) {

            // 1、解析allServerAddress地址，并且临时存储到我们的newAllServerNodeSet中
            Set<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
            for (int i = 0; i < allServerAdress.size(); i++) {
                String[] array = allServerAdress.get(i).split(":");
                if (array.length == 2) {
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                    newAllServerNodeSet.add(remotePeer);
                }
            }

            // 2、建立连接方法，发起远程连接操作
            for (InetSocketAddress socketAddress : newAllServerNodeSet) {
                if (!connectedHandlerMap.containsKey(socketAddress)) {
                    connectAsync(socketAddress);
                }
            }

            // 3、如果newAllServerNodeSet列表里不存在的地址，那么我需要从缓存中进行移除
            for (int i = 0; i < connectedHandlerList.size(); i++) {
                RpcClientHandler rpcClientHandler = connectedHandlerList.get(i);
                SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
                if (newAllServerNodeSet.contains(remotePeer)) {
                    log.info("remove invalid server node: {}", remotePeer);
                    RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
                    if (Objects.nonNull(handler)) {
                        handler.close();
                        connectedHandlerMap.remove(remotePeer);
                    }
                    connectedHandlerList.remove(rpcClientHandler);
                }
            }
        } else {
            // 添加告警
            log.error("no available server address!!");
            // 清除所有的缓存信息
            clearConnected();
        }
    }

    /**
     * 异步发起连接的方法
     *
     * @param remotePeer 要发起链接的地址和端口
     * @author debao.yang
     * @since 2024/7/2 06:01
     */
    private void connectAsync(InetSocketAddress remotePeer) {
        executor.submit(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new RpcClientInitializer());
            connect(bootstrap, remotePeer);
        });
    }

    private void connect(Bootstrap b, InetSocketAddress remotePeer) {
        // 1、真正的建立连接
        ChannelFuture channelFuture = b.connect(remotePeer);

        // 2、连接失败的时候添加监听，清除资源后进行发起重连操作
        channelFuture.channel()
                .closeFuture()
                .addListener((ChannelFutureListener) future -> {
                    log.info("channelFuture.channel close operationComplete, remote " +
                            "peer = {}", remotePeer);
                    future.channel().eventLoop().schedule(() -> {
                                log.warn("connect fail ,to connect!");
                                clearConnected();
                                connect(b, remotePeer);
                            }, 3,
                            TimeUnit.SECONDS);
                });

        // 3、连接成功的时候添加监听，把新链接放入缓存中
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Successfully connet to remote server, remote peer =" +
                        " {}", remotePeer);
                RpcClientHandler handler = future.channel()
                        .pipeline()
                        .get(RpcClientHandler.class);
                addHandler(handler);
            }
        });
    }

    /**
     * 连接失败时，即使的释放资源，清空缓存
     * 先删除connectedHandlerMap中的数据
     * 然后再清空connectedHandlerList的数据
     *
     * @author debao.yang
     * @since 2024/7/2 06:25
     */
    private void clearConnected() {
        for (RpcClientHandler rpcClientHandler : connectedHandlerList) {
            // 通过RpcClientHandler 找到具体的 remotePeer，从connectedHandlerMap进行移除指定的RpcClientHandler
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
            if (Objects.nonNull(handler)) {
                handler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        }
        connectedHandlerList.clear();
    }

    /**
     * 添加RpcClientHandler到指定的缓存中
     * 要添加的缓存有：connectedHandlerMap & connectedHandlerList
     *
     * @param handler 实际的业务处理器
     * @author debao.yang
     * @since 2024/7/2 06:52
     */
    private void addHandler(RpcClientHandler handler) {
        connectedHandlerList.add(handler);
        SocketAddress address = // handler.getRemotePeer();
                handler.getChannel().remoteAddress();
                connectedHandlerMap.put(((InetSocketAddress) address), handler);

        // signalAvailableHandler 唤醒可用的业务执行器
        signalAvailableHandler();
    }

    /**
     * 唤醒另外一端的线程(阻塞的状态中) 告知有新链接接入
     *
     * @author debao.yang
     * @since 2024/7/2 06:58
     */
    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 等待新的链接接入通知方法
     *
     * @return boolean
     * @author debao.yang
     * @since 2024/7/2 07:18
     */
    private boolean waitingForAvailableHandler() throws InterruptedException {

        connectedLock.lock();
        try {
            return connectedCondition.await(this.connectTimeoutMills,
                    TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 选择一个实际的业务处理器
     *
     * @return com.rpc.netty.rapid.client.RpcClientHandler
     * @author debao.yang
     * @since 2024/7/2 07:14
     */
    @SuppressWarnings("unchecked")
    public RpcClientHandler chooseHandler() {
        // 复制出来，保证线程安全问题
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();

        int size = handlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = waitingForAvailableHandler();
                if (available) {
                    handlers =
                            ((CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone());
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                log.error(" waiting for available node is interrupted!");
                throw new RuntimeException("no connect any server! ", e);
            }
        }

        if (!isRunning) {
            return null;
        }
        // 最终使用取模方式取得其中一个业务处理器进行实际的业务处理
        int index = (handlerIdx.getAndAdd(1) + size) % size;
        return handlers.get(index);
    }

    /**
     * 关闭的方法
     *
     * @author debao.yang
     * @since 2024/7/2 07:41
     */
    public void stop() {
        isRunning = false;
        for (RpcClientHandler handler : connectedHandlerList) {
            handler.close();
        }
        // 在这里要调用一下唤醒操作
        signalAvailableHandler();
        executor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * 发起重连的方法 需要把对应的资源进行释放
     *
     * @param handler    实际的业务处理器
     * @param remotePeer 远程连接资源
     * @author debao.yang
     * @since 2024/7/2 07:45
     */
    public void reconnect(RpcClientHandler handler, SocketAddress remotePeer) {
        if (handler != null) {
            handler.close();
            connectedHandlerList.remove(handler);
            connectedHandlerMap.remove(remotePeer);
        }
        connectAsync((InetSocketAddress) remotePeer);
    }

}
