package com.fix.test.service;

import com.fix.test.entity.TestPlan;
import com.fix.test.entity.TestTask;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FixPressureService {
    
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, TestTaskContext> taskContexts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(20);
    
    // 监控数据收集
    private final AtomicLong totalSent = new AtomicLong(0);
    private final AtomicLong totalReceived = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    
    public static class TestTaskContext {
        private final String taskId;
        private final TestPlan plan;
        private volatile boolean running = true;
        private volatile boolean paused = false;
        private final AtomicInteger currentTps = new AtomicInteger(0);
        private final AtomicLong currentStepStartTime = new AtomicLong(0);
        private ScheduledFuture<?> currentTask;
        
        public TestTaskContext(String taskId, TestPlan plan) {
            this.taskId = taskId;
            this.plan = plan;
        }
        
        // Getters and Setters
    }
    
    /**
     * 初始化并发FIX会话
     */
    public void initializeSessions(String taskId, TestPlan plan) throws ConfigError {
        SessionSettings settings = createSessionSettings(plan);
        
        for (int i = 1; i <= plan.getSessionCount(); i++) {
            String sessionId = plan.getPlanName() + "_" + i;
            SessionID sessionID = new SessionID(
                plan.getFixVersion().toString(), 
                "CLIENT_" + String.format("%03d", i), 
                "SERVER"
            );
            
            // 创建会话配置
            Session session = SessionFactory.createSession(settings, sessionID);
            sessions.put(sessionId, session);
        }
    }
    
    /**
     * 开始压力测试
     */
    public void startPressureTest(String taskId, TestPlan plan) {
        TestTaskContext context = new TestTaskContext(taskId, plan);
        taskContexts.put(taskId, context);
        
        // 按TPS阶梯执行
        List<TestPlan.TpsStep> steps = plan.getTpsSteps();
        for (int i = 0; i < steps.size(); i++) {
            TestPlan.TpsStep step = steps.get(i);
            scheduleTpsStep(taskId, step, i == steps.size() - 1);
        }
    }
    
    /**
     * 调度TPS阶梯
     */
    private void scheduleTpsStep(String taskId, TestPlan.TpsStep step, boolean isLastStep) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context == null || !context.running) return;
        
        context.currentTps.set(step.getTps());
        context.currentStepStartTime.set(System.currentTimeMillis());
        
        // 计算每条消息的发送间隔
        long intervalMs = 1000L / step.getTps();
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (!context.running || context.paused) return;
            
            // 发送消息
            sendMessagesForStep(taskId, step);
            
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
        
        context.currentTask = task;
        
        // 调度下一步或结束
        if (!isLastStep) {
            scheduler.schedule(() -> {
                task.cancel(false);
                // 继续下一步
            }, step.getDuration(), TimeUnit.MINUTES);
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessagesForStep(String taskId, TestPlan.TpsStep step) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context == null) return;
        
        // 根据消息配置发送不同类型的消息
        List<MessageConfig> configs = context.plan.getMessageConfigs();
        
        for (MessageConfig config : configs) {
            int messageCount = (int) (step.getTps() * config.getMsgRatio() / 100.0);
            
            for (int i = 0; i < messageCount; i++) {
                messageExecutor.submit(() -> {
                    try {
                        sendFixMessage(taskId, config);
                        totalSent.incrementAndGet();
                    } catch (Exception e) {
                        totalFailed.incrementAndGet();
                    }
                });
            }
        }
    }
    
    /**
     * 发送FIX消息
     */
    private void sendFixMessage(String taskId, MessageConfig config) throws SessionNotFound {
        // 创建不同类型的FIX消息
        switch (config.getMsgType()) {
            case "D": // NewOrderSingle
                sendNewOrderSingle(taskId, config);
                break;
            case "F": // CancelOrder
                sendCancelOrder(taskId, config);
                break;
            case "G": // ReplaceOrder
                sendReplaceOrder(taskId, config);
                break;
            default:
                sendHeartbeat(taskId);
        }
    }
    
    /**
     * 创建新订单消息
     */
    private void sendNewOrderSingle(String taskId, MessageConfig config) throws SessionNotFound {
        NewOrderSingle order = new NewOrderSingle();
        
        // 设置基本字段
        order.set(new ClOrdID(UUID.randomUUID().toString()));
        order.set(new HandlInst('1'));
        order.set(new Symbol("AAPL"));
        order.set(new Side(Side.BUY));
        order.set(new TransactTime(new Date()));
        order.set(new OrderQty(100));
        order.set(new OrdType(OrdType.MARKET));
        
        // 发送消息
        Session.sendToTarget(order, getRandomSession(taskId));
    }
    
    /**
     * 获取随机会话
     */
    private SessionID getRandomSession(String taskId) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context == null) return null;
        
        int sessionIndex = (int) (Math.random() * context.plan.getSessionCount()) + 1;
        return new SessionID(
            context.plan.getFixVersion().toString(),
            "CLIENT_" + String.format("%03d", sessionIndex),
            "SERVER"
        );
    }
    
    /**
     * 暂停测试
     */
    public void pauseTest(String taskId) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context != null) {
            context.paused = true;
            if (context.currentTask != null) {
                context.currentTask.cancel(false);
            }
        }
    }
    
    /**
     * 恢复测试
     */
    public void resumeTest(String taskId) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context != null) {
            context.paused = false;
            // 重新调度当前阶梯
            TestPlan.TpsStep currentStep = getCurrentStep(context);
            scheduleTpsStep(taskId, currentStep, false);
        }
    }
    
    /**
     * 停止测试
     */
    public void stopTest(String taskId) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context != null) {
            context.running = false;
            context.paused = false;
            if (context.currentTask != null) {
                context.currentTask.cancel(true);
            }
            taskContexts.remove(taskId);
        }
    }
    
    /**
     * 获取实时监控数据
     */
    public Map<String, Object> getRealTimeMetrics(String taskId) {
        TestTaskContext context = taskContexts.get(taskId);
        if (context == null) return Collections.emptyMap();
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("taskId", taskId);
        metrics.put("currentTps", context.currentTps.get());
        metrics.put("totalSent", totalSent.get());
        metrics.put("totalReceived", totalReceived.get());
        metrics.put("totalFailed", totalFailed.get());
        metrics.put("sessionCount", context.plan.getSessionCount());
        metrics.put("isRunning", context.running);
        metrics.put("isPaused", context.paused);
        
        return metrics;
    }
    
    /**
     * 创建会话设置
     */
    private SessionSettings createSessionSettings(TestPlan plan) {
        SessionSettings settings = new SessionSettings();
        
        // 基本设置
        settings.setString("ConnectionType", "initiator");
        settings.setString("StartTime", "00:00:00");
        settings.setString("EndTime", "00:00:00");
        settings.setString("HeartBtInt", "30");
        settings.setString("ReconnectInterval", "5");
        settings.setString("FileStorePath", "data/fix");
        settings.setString("FileLogPath", "log/fix");
        
        return settings;
    }
    
    // 辅助方法
    private void sendCancelOrder(String taskId, MessageConfig config) {
        // 实现取消订单消息
    }
    
    private void sendReplaceOrder(String taskId, MessageConfig config) {
        // 实现替换订单消息
    }
    
    private void sendHeartbeat(String taskId) {
        // 实现心跳消息
    }
    
    private TestPlan.TpsStep getCurrentStep(TestTaskContext context) {
        // 获取当前阶梯配置
        return context.plan.getTpsSteps().get(0);
    }
}