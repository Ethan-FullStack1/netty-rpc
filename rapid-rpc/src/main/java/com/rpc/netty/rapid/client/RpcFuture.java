package com.rpc.netty.rapid.client;

import com.rpc.netty.rapid.codec.RpcRequest;
import com.rpc.netty.rapid.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author debao.yang
 * @since 2024/7/2 12:11
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest request;
    private RpcResponse rpcResponse;

    private long startTime;

    private final long TIME_THRESHOLD = 5000;

    private final List<RpcCallBack> pendingCallbacks = new ArrayList<>();

    private Sync sync;

    private final Lock lock = new ReentrantLock();

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16,
            60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.sync = new Sync();
    }

    /**
     * 实际的回调处理
     *
     * @param rpcResponse rpc返回的信息
     * @author debao.yang
     * @since 2024/7/2 12:28
     */
    public void done(RpcResponse rpcResponse) {
        this.rpcResponse = rpcResponse;
        boolean success = sync.release(1);
        if (success) {
            invokeCallbacks();
        }
        // 整体rpc调用的耗时时间
        long costTime = System.currentTimeMillis() - startTime;
        if (TIME_THRESHOLD < costTime) {
            log.warn("the rpc response time is too slow!! request id = {}, " +
                            "cost time = {}",
                    request.getRequestId(), costTime);
        }
    }

    /**
     * 依次执行回调函数处理
     *
     * @author debao.yang
     * @since 2024/7/2 13:36
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (RpcCallBack callBack : pendingCallbacks) {
                runCallback(callBack);
            }
        } finally {
            lock.unlock();
        }
    }

    private void runCallback(RpcCallBack callBack) {
        final RpcResponse response = this.rpcResponse;
        executor.submit(() -> {
            if (response.getThrowable() == null) {
                callBack.success(response.getResult());
            } else {
                callBack.failure(response.getThrowable());
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.rpcResponse != null) {
            return this.rpcResponse.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout,
                      TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.rpcResponse != null) {
                return this.rpcResponse.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("timeout exception requestid:  " + request.getRequestId() + "，className: " + request.getClassName() + "，methodName: " + request.getMethodName());
        }
    }

    class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -527388158554160853L;

        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);
            }
            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }
    }

    /**
     * 可以在应用执行的过程中添加回到处理
     *
     * @param callBack 回调函数
     * @return com.rpc.netty.rapid.client.RpcFuture
     * @author debao.yang
     * @since 2024/7/2 13:44
     */
    public RpcFuture addCallback(RpcCallBack callBack) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callBack);
            } else {
                this.pendingCallbacks.add(callBack);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
}
