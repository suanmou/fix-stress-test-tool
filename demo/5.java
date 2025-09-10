package com.fixstress;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixStressTestClient implements Application {
    private static final Logger logger = LoggerFactory.getLogger(FixStressTestClient.class);
    
    private Initiator initiator;
    private SessionSettings settings;
    private MessageFactory messageFactory;
    private FileStoreFactory storeFactory;
    private FileLogFactory logFactory;
    
    private TestConfig config;
    private TestStatistics statistics;
    private FixMessageGenerator messageGenerator;
    
    private ScheduledExecutorService scheduler;
    private AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch completionLatch;
    
    public FixStressTestClient(TestConfig config) {
        this.config = config;
        this.statistics = new TestStatistics();
        this.messageGenerator = new FixMessageGenerator(config);
    }
    
    public void initialize() throws ConfigError {
        try {
            // 创建会话设置
            settings = new SessionSettings();
            
            // 设置默认会话配置
            Dictionary defaultSettings = new Dictionary();
            defaultSettings.setString("ConnectionType", "initiator");
            defaultSettings.setString("SocketConnectProtocol", "TCP");
            defaultSettings.setString("StartTime", "00:00:00");
            defaultSettings.setString("EndTime", "00:00:00");
            defaultSettings.setString("HeartBtInt", "30");
            defaultSettings.setString("ReconnectInterval", "5");
            defaultSettings.setBool("ResetOnLogon", true);
            defaultSettings.setBool("ResetOnLogout", true);
            defaultSettings.setBool("ResetOnDisconnect", true);
            settings.set(defaultSettings);
            
            // 创建会话
            SessionID sessionId = new SessionID(
                "FIX.4.4", 
                config.getSenderId(), 
                config.getTargetId()
            );
            
            Dictionary sessionSettings = new Dictionary();
            sessionSettings.setString("SocketConnectHost", config.getHost());
            sessionSettings.setInt("SocketConnectPort", config.getPort());
            settings.set(sessionId, sessionSettings);
            
            // 创建工厂
            messageFactory = new DefaultMessageFactory();
            storeFactory = new FileStoreFactory(settings);
            logFactory = new FileLogFactory(settings);
            
            // 创建启动器
            initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
        } catch (Exception e) {
            logger.error("Failed to initialize FIX client", e);
            throw new ConfigError("FIX client initialization failed", e);
        }
    }
    
    public void startTest() throws Exception {
        if (running.get()) {
            logger.warn("Test is already running");
            return;
        }
        
        logger.info("Starting FIX stress test with configuration: {}", config);
        
        try {
            // 启动FIX连接
            initiator.start();
            running.set(true);
            statistics.start();
            
            // 等待连接建立
            Thread.sleep(2000);
            
            // 创建定时任务发送消息
            scheduler = Executors.newScheduledThreadPool(2);
            completionLatch = new CountDownLatch(1);
            
            // 计算发送间隔和批处理大小
            int batchSize = config.getBatchSize();
            long intervalMs = 1000L / (config.getMsgRate() / batchSize);
            
            logger.info("Sending {} messages per batch every {} ms", batchSize, intervalMs);
            
            // 安排发送任务
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                this::sendMessageBatch, 
                0, intervalMs, TimeUnit.MILLISECONDS
            );
            
            // 安排停止任务
            scheduler.schedule(() -> {
                future.cancel(true);
                stopTest();
                completionLatch.countDown();
            }, config.getDuration(), TimeUnit.SECONDS);
            
            // 等待测试完成
            completionLatch.await();
            
        } catch (Exception e) {
            logger.error("Error during stress test", e);
            stopTest();
            throw e;
        }
    }
    
    private void sendMessageBatch() {
        if (!running.get()) return;
        
        try {
            SessionID sessionId = initiator.getSessions().get(0);
            int batchSize = config.getBatchSize();
            
            for (int i = 0; i < batchSize; i++) {
                Message message = messageGenerator.createMessage(sessionId);
                long startTime = System.currentTimeMillis();
                
                // 发送消息
                Session.sendToTarget(message, sessionId);
                statistics.incrementMessagesSent();
                
                // 记录发送时间用于延迟计算
                message.setField(new StringField(9998, String.valueOf(startTime)));
            }
        } catch (Exception e) {
            logger.error("Error sending message batch", e);
            statistics.incrementErrorCount();
        }
    }
    
    public void stopTest() {
        if (!running.getAndSet(false)) return;
        
        logger.info("Stopping FIX stress test");
        
        try {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            }
            
            if (initiator != null) {
                initiator.stop();
            }
            
            statistics.stop();
            logger.info("Test completed. Statistics: {}", statistics);
            
        } catch (Exception e) {
            logger.error("Error stopping test", e);
        }
    }
    
    public TestStatistics getStatistics() {
        return statistics;
    }
    
    // Application interface methods
    @Override
    public void onCreate(SessionID sessionId) {
        logger.info("Session created: {}", sessionId);
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        logger.info("Logged on: {}", sessionId);
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        logger.info("Logged out: {}", sessionId);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // 可以在这里添加管理消息的处理逻辑
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) 
        throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理接收到的管理消息
    }
    
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // 在发送应用消息前的处理逻辑
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) 
        throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        
        // 处理接收到的应用消息
        statistics.incrementMessagesReceived();
        
        try {
            // 如果是执行报告，计算延迟
            if (message instanceof ExecutionReport) {
                String sendTimeStr = message.getString(9998);
                if (sendTimeStr != null) {
                    long sendTime = Long.parseLong(sendTimeStr);
                    long receiveTime = System.currentTimeMillis();
                    long latency = receiveTime - sendTime;
                    statistics.recordLatency(latency);
                }
            }
        } catch (Exception e) {
            logger.warn("Error processing received message", e);
        }
    }
}