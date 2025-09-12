package com.finance.fix.tester;

import quickfix.*;
import quickfix.field.TestReqID;
import quickfix.fix44.Heartbeat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TestSessionApplication implements Application {
    private final int sessionId;
    private final long timeoutMillis;
    private final Callback<Long> responseCallback;
    private final Runnable connectionEstablishedCallback;
    private final Callback<String> connectionFailedCallback;
    private final Runnable reconnectionCallback;
    
    // 会话状态跟踪
    private final AtomicBoolean isLoggedOn = new AtomicBoolean(false);
    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private final Map<String, Long> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutChecker = Executors.newSingleThreadScheduledExecutor();

    // 函数式接口：回调
    @FunctionalInterface
    public interface Callback<T> {
        void call(T value);
    }

    public TestSessionApplication(int sessionId, long timeoutMillis,
                                 Callback<Long> responseCallback,
                                 Runnable connectionEstablishedCallback,
                                 Callback<String> connectionFailedCallback,
                                 Runnable reconnectionCallback) {
        this.sessionId = sessionId;
        this.timeoutMillis = timeoutMillis;
        this.responseCallback = responseCallback;
        this.connectionEstablishedCallback = connectionEstablishedCallback;
        this.connectionFailedCallback = connectionFailedCallback;
        this.reconnectionCallback = reconnectionCallback;
        
        // 启动超时检查器（每1秒检查一次）
        timeoutChecker.scheduleAtFixedRate(this::checkTimeouts, 1, 1, TimeUnit.SECONDS);
    }

    // 等待登录完成
    public boolean waitForLogon(long timeout) throws InterruptedException {
        return logonLatch.await(timeout, TimeUnit.MILLISECONDS);
    }

    // 跟踪请求消息
    public void trackRequest(String testReqID, long sendTime) {
        pendingRequests.put(testReqID, sendTime);
    }

    // 检查超时请求
    private void checkTimeouts() {
        long currentTime = System.currentTimeMillis();
        List<String> timedOutRequests = pendingRequests.entrySet().stream()
            .filter(entry -> currentTime - entry.getValue() > timeoutMillis)
            .map(Map.Entry::getKey)
            .toList();
        
        for (String testReqID : timedOutRequests) {
            pendingRequests.remove(testReqID);
            FixPressureTester.totalTimeouts.incrementAndGet();
            // System.out.println("Session " + sessionId + " request " + testReqID + " timed out");
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
        // 会话创建时调用
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Session " + this.sessionId + " logged on successfully");
        isLoggedOn.set(true);
        logonLatch.countDown();
        connectionEstablishedCallback.run();
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Session " + this.sessionId + " logged out");
        isLoggedOn.set(false);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // 发送管理消息时调用（如登录请求）
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理收到的管理消息
        if (message instanceof Heartbeat) {
            try {
                Heartbeat heartbeat = (Heartbeat) message;
                TestReqID testReqID = heartbeat.getTestReqID();
                String reqId = testReqID.getValue();
                
                // 查找并处理匹配的请求
                Long sendTime = pendingRequests.remove(reqId);
                if (sendTime != null) {
                    long responseTime = System.currentTimeMillis() - sendTime;
                    responseCallback.call(responseTime);
                }
            } catch (FieldNotFound e) {
                // 没有TestReqID的心跳消息，忽略
            }
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // 发送应用消息时调用
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // 处理收到的应用消息（TestRequest通常用Heartbeat响应，这里可能不需要处理）
    }
    
    // 清理资源
    public void shutdown() {
        timeoutChecker.shutdown();
    }
}
    