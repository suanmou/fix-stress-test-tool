package com.finance.fix.tester;

import quickfix.*;
import quickfix.field.TestReqID;
import quickfix.fix44.TestRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionManager implements Runnable {
    private final SessionSettings settings;
    private final int messagesPerSession;
    private final int messagesPerSecond;
    private final int durationMinutes;
    private final long timeoutMillis;
    private final ResponseListener responseListener;
    
    private Initiator initiator;
    private final MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
    private final LogFactory logFactory = new ScreenLogFactory(true, true, true);
    private final MessageFactory messageFactory = new DefaultMessageFactory();
    
    // 跟踪请求的映射表: TestReqID -> 发送时间
    private final Map<String, Instant> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger sentCount = new AtomicInteger(0);
    private final AtomicInteger timeoutCount = new AtomicInteger(0);
    
    public SessionManager(SessionSettings settings, int messagesPerSession, 
                         int messagesPerSecond, int durationMinutes,
                         long timeoutMillis, ResponseListener responseListener) {
        this.settings = settings;
        this.messagesPerSession = messagesPerSession;
        this.messagesPerSecond = messagesPerSecond;
        this.durationMinutes = durationMinutes;
        this.timeoutMillis = timeoutMillis;
        this.responseListener = responseListener;
    }
    
    @Override
    public void run() {
        try {
            // 创建自定义应用处理器
            TestApplication application = new TestApplication(this::onHeartbeatReceived);
            
            // 初始化并启动 initiator
            initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory, messageFactory);
            initiator.start();
            
            System.out.println("会话 " + getSessionId() + " 已启动");
            
            // 控制发送速率的限流器
            RateLimiter rateLimiter = RateLimiter.create(messagesPerSecond);
            
            // 确定测试结束条件
            Instant endTime = durationMinutes > 0 ? 
                Instant.now().plus(Duration.ofMinutes(durationMinutes)) : 
                null;
            
            // 发送测试消息
            boolean continueSending = true;
            while (continueSending) {
                // 检查是否达到消息数量限制
                if (messagesPerSession > 0 && sentCount.get() >= messagesPerSession) {
                    break;
                }
                
                // 检查是否达到时间限制
                if (endTime != null && Instant.now().isAfter(endTime)) {
                    break;
                }
                
                // 等待令牌，控制发送速率
                rateLimiter.acquire();
                
                // 发送TestRequest消息
                sendTestRequest();
            }
            
            // 等待所有响应或超时
            if (durationMinutes > 0) {
                // 额外等待一段时间处理最后响应
                Thread.sleep(timeoutMillis);
            }
            
            // 停止会话
            initiator.stop();
            System.out.println("会话 " + getSessionId() + " 已结束. 发送: " + sentCount.get());
            
        } catch (Exception e) {
            System.err.println("会话 " + getSessionId() + " 发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendTestRequest() throws SessionNotFound {
        // 生成唯一的TestReqID
        String testReqID = UUID.randomUUID().toString().substring(0, 10);
        
        // 创建TestRequest消息
        TestRequest testRequest = new TestRequest();
        testRequest.set(new TestReqID(testReqID));
        
        // 发送消息
        SessionID sessionId = initiator.getSessions().get(0);
        boolean sent = Session.sendToTarget(testRequest, sessionId);
        
        if (sent) {
            // 记录发送时间，用于计算响应时间
            pendingRequests.put(testReqID, Instant.now());
            sentCount.incrementAndGet();
            
            // 更新全局计数器
            ((FixPressureTester)Thread.currentThread().getThreadGroup().getParent()).totalSent.incrementAndGet();
        }
    }
    
    // 处理收到的Heartbeat响应
    private void onHeartbeatReceived(String testReqID) {
        Instant sendTime = pendingRequests.remove(testReqID);
        if (sendTime != null) {
            // 计算响应时间并通知监听器
            long responseTime = Duration.between(sendTime, Instant.now()).toMillis();
            responseListener.onResponse(responseTime);
        }
    }
    
    // 检查超时的请求
    public int getTimeoutCount() {
        Instant now = Instant.now();
        int count = 0;
        
        for (Map.Entry<String, Instant> entry : pendingRequests.entrySet()) {
            if (Duration.between(entry.getValue(), now).toMillis() > timeoutMillis) {
                count++;
            }
        }
        
        timeoutCount.set(count);
        return count;
    }
    
    private String getSessionId() {
        try {
            return initiator != null && !initiator.getSessions().isEmpty() ? 
                initiator.getSessions().get(0).toString() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }
    
    // 响应监听器接口
    @FunctionalInterface
    public interface ResponseListener {
        void onResponse(long responseTime);
    }
}
    