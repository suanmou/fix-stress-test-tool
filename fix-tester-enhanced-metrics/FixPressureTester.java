package com.finance.fix.tester;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.Heartbeat;
import quickfix.fix44.TestRequest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FixPressureTester {
    private final String configTemplatePath;
    private final int sessionCount;
    private final int messagesPerSession;
    private final int tps;
    private final int durationMinutes;
    private final boolean isDurationMode;
    private final long timeoutMillis = 5000;
    private final String outputReportPath;
    
    // 全局统计指标
    private final AtomicLong totalConnectionsAttempted = new AtomicLong(0);
    private final AtomicLong totalConnectionsSucceeded = new AtomicLong(0);
    private final AtomicLong totalConnectionsFailed = new AtomicLong(0);
    private final AtomicLong totalReconnections = new AtomicLong(0);
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalResponsesReceived = new AtomicLong(0);
    private final AtomicLong totalTimeouts = new AtomicLong(0);
    private final List<Long> allResponseTimes = new CopyOnWriteArrayList<>();
    private final List<Long> connectionEstablishmentTimes = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> connectionFailureReasons = new ConcurrentHashMap<>();
    
    // 系统资源监控
    private final ScheduledExecutorService systemMonitor = Executors.newScheduledThreadPool(1);
    private final List<SystemMetrics> systemMetricsList = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public FixPressureTester(String configTemplatePath, int sessionCount, int messagesPerSession, 
                            int tps, int durationMinutes, String outputReportPath) {
        this.configTemplatePath = configTemplatePath;
        this.sessionCount = sessionCount;
        this.messagesPerSession = messagesPerSession;
        this.tps = tps;
        this.durationMinutes = durationMinutes;
        this.isDurationMode = durationMinutes > 0;
        this.outputReportPath = outputReportPath;
    }

    public void startTest() throws Exception {
        System.out.println("Starting FIX pressure test with " + sessionCount + " sessions...");
        System.out.println("Configuration: " + (isDurationMode ? 
            "Duration mode - " + durationMinutes + " minutes, " + tps + " TPS" : 
            "Message count mode - " + messagesPerSession + " messages, " + tps + " TPS"));
        
        // 启动系统资源监控（每5秒采集一次）
        systemMonitor.scheduleAtFixedRate(this::recordSystemMetrics, 0, 5, TimeUnit.SECONDS);
        
        // 启动所有会话
        ExecutorService sessionExecutor = Executors.newFixedThreadPool(sessionCount);
        List<Future<?>> sessionFutures = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();
        long testEndTime = isDurationMode ? 
            testStartTime + (durationMinutes * 60 * 1000) : Long.MAX_VALUE;
        
        for (int i = 0; i < sessionCount; i++) {
            final int sessionId = i;
            Future<?> future = sessionExecutor.submit(() -> {
                try {
                    runSession(sessionId, testEndTime);
                } catch (Exception e) {
                    System.err.println("Session " + sessionId + " failed: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            sessionFutures.add(future);
        }
        
        // 等待所有会话完成
        for (Future<?> future : sessionFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error waiting for session completion: " + e.getMessage());
            }
        }
        
        // 停止系统监控
        systemMonitor.shutdown();
        systemMonitor.awaitTermination(1, TimeUnit.MINUTES);
        
        // 生成测试报告
        generateTestReport(testStartTime, System.currentTimeMillis());
        
        System.out.println("Test completed. Results written to " + outputReportPath);
    }

    private void runSession(int sessionId, long testEndTime) throws Exception {
        String sessionConfigPath = createSessionConfig(sessionId);
        SessionSettings settings = new SessionSettings(new File(sessionConfigPath));
        
        // 连接状态跟踪
        long connectionAttemptTime = System.currentTimeMillis();
        totalConnectionsAttempted.incrementAndGet();
        
        TestSessionApplication application = new TestSessionApplication(sessionId, timeoutMillis,
                this::onResponseReceived, this::onConnectionEstablished, this::onConnectionFailed,
                this::onReconnection);
        
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        Initiator initiator = new SocketInitiator(application, storeFactory, settings, logFactory, messageFactory);
        
        try {
            initiator.start();
            System.out.println("Session " + sessionId + " started. Waiting for logon...");
            
            // 等待连接建立或超时（30秒）
            if (!application.waitForLogon(30000)) {
                String reason = "Logon timeout after 30 seconds";
                recordConnectionFailure(reason);
                System.err.println("Session " + sessionId + " failed to logon: " + reason);
                return;
            }
            
            // 连接成功，记录建立时间
            long connectionEstablishedTime = System.currentTimeMillis() - connectionAttemptTime;
            connectionEstablishmentTimes.add(connectionEstablishedTime);
            
            // 开始发送消息
            RateLimiter rateLimiter = new RateLimiter(tps);
            int messagesSent = 0;
            
            while ((!isDurationMode && messagesSent < messagesPerSession) || 
                   (isDurationMode && System.currentTimeMillis() < testEndTime)) {
                // 控制发送速率
                rateLimiter.acquire();
                
                // 创建并发送TestRequest
                String testReqID = "TEST_" + sessionId + "_" + System.currentTimeMillis();
                TestRequest testRequest = new TestRequest();
                testRequest.set(new TestReqID(testReqID));
                
                SessionID sessionID = initiator.getSessions().get(0);
                if (Session.sendToTarget(testRequest, sessionID)) {
                    totalMessagesSent.incrementAndGet();
                    messagesSent++;
                    application.trackRequest(testReqID, System.currentTimeMillis());
                    
                    // 每100条消息打印一次进度
                    if (messagesSent % 100 == 0) {
                        System.out.println("Session " + sessionId + " sent " + messagesSent + " messages");
                    }
                } else {
                    System.err.println("Session " + sessionId + " failed to send message " + messagesSent);
                }
            }
            
            System.out.println("Session " + sessionId + " finished. Sent " + messagesSent + " messages");
            
            // 等待剩余响应（最长超时时间）
            Thread.sleep(timeoutMillis);
            
        } finally {
            initiator.stop();
            new File(sessionConfigPath).delete(); // 清理临时配置文件
        }
    }
    
    // 回调方法：处理响应接收
    private void onResponseReceived(long responseTime) {
        totalResponsesReceived.incrementAndGet();
        allResponseTimes.add(responseTime);
    }
    
    // 回调方法：处理连接建立
    private void onConnectionEstablished() {
        totalConnectionsSucceeded.incrementAndGet();
    }
    
    // 回调方法：处理连接失败
    private void onConnectionFailed(String reason) {
        totalConnectionsFailed.incrementAndGet();
        recordConnectionFailure(reason);
    }
    
    // 回调方法：处理重连
    private void onReconnection() {
        totalReconnections.incrementAndGet();
    }
    
    // 记录连接失败原因
    private void recordConnectionFailure(String reason) {
        connectionFailureReasons.put(reason, connectionFailureReasons.getOrDefault(reason, 0) + 1);
    }
    
    // 记录系统资源指标
    private void recordSystemMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        SystemMetrics metrics = new SystemMetrics(
            System.currentTimeMillis(),
            osBean.getSystemCpuLoad() * 100,
            osBean.getFreePhysicalMemorySize(),
            osBean.getTotalPhysicalMemorySize(),
            ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()
        );
        systemMetricsList.add(metrics);
    }
    
    // 生成测试报告
    private void generateTestReport(long startTime, long endTime) throws IOException {
        try (FileWriter writer = new FileWriter(outputReportPath)) {
            long testDuration = endTime - startTime;
            
            // 计算响应时间统计
            double avgResponseTime = allResponseTimes.stream().mapToLong(l -> l).average().orElse(0);
            long minResponseTime = allResponseTimes.stream().mapToLong(l -> l).min().orElse(0);
            long maxResponseTime = allResponseTimes.stream().mapToLong(l -> l).max().orElse(0);
            long p95ResponseTime = calculatePercentile(allResponseTimes, 95);
            long p99ResponseTime = calculatePercentile(allResponseTimes, 99);
            
            // 计算连接时间统计
            double avgConnectionTime = connectionEstablishmentTimes.stream().mapToLong(l -> l).average().orElse(0);
            long minConnectionTime = connectionEstablishmentTimes.stream().mapToLong(l -> l).min().orElse(0);
            long maxConnectionTime = connectionEstablishmentTimes.stream().mapToLong(l -> l).max().orElse(0);
            
            // 汇总系统资源指标
            double avgCpuUsage = systemMetricsList.stream().mapToDouble(m -> m.cpuUsage).average().orElse(0);
            double avgHeapUsage = systemMetricsList.stream().mapToLong(m -> m.heapUsed).average().orElse(0);

            // 写入报告
            writer.write("FIX压力测试报告 - " + sdf.format(new Date()) + "\n");
            writer.write("========================================\n\n");
            
            writer.write("测试配置:\n");
            writer.write("  会话数量: " + sessionCount + "\n");
            writer.write("  测试模式: " + (isDurationMode ? "时长模式 (" + durationMinutes + "分钟)" : "消息数量模式 (" + messagesPerSession + "条/会话)") + "\n");
            writer.write("  发送速率: " + tps + " TPS\n");
            writer.write("  测试时长: " + (testDuration / 1000) + "秒\n\n");
            
            writer.write("连接统计:\n");
            writer.write("  尝试连接数: " + totalConnectionsAttempted.get() + "\n");
            writer.write("  成功连接数: " + totalConnectionsSucceeded.get() + " (" + 
                       String.format("%.2f", (totalConnectionsSucceeded.get() * 100.0 / totalConnectionsAttempted.get())) + "%)\n");
            writer.write("  失败连接数: " + totalConnectionsFailed.get() + " (" + 
                       String.format("%.2f", (totalConnectionsFailed.get() * 100.0 / totalConnectionsAttempted.get())) + "%)\n");
            writer.write("  重连次数: " + totalReconnections.get() + "\n");
            writer.write("  平均连接建立时间: " + String.format("%.2f", avgConnectionTime) + "ms\n");
            writer.write("  最小连接建立时间: " + minConnectionTime + "ms\n");
            writer.write("  最大连接建立时间: " + maxConnectionTime + "ms\n\n");
            
            writer.write("消息统计:\n");
            writer.write("  总发送消息数: " + totalMessagesSent.get() + "\n");
            writer.write("  总响应消息数: " + totalResponsesReceived.get() + " (" + 
                       String.format("%.2f", (totalResponsesReceived.get() * 100.0 / totalMessagesSent.get())) + "%)\n");
            writer.write("  超时消息数: " + totalTimeouts.get() + " (" + 
                       String.format("%.2f", (totalTimeouts.get() * 100.0 / totalMessagesSent.get())) + "%)\n");
            writer.write("  平均响应时间: " + String.format("%.2f", avgResponseTime) + "ms\n");
            writer.write("  最小响应时间: " + minResponseTime + "ms\n");
            writer.write("  最大响应时间: " + maxResponseTime + "ms\n");
            writer.write("  95%响应时间: " + p95ResponseTime + "ms\n");
            writer.write("  99%响应时间: " + p99ResponseTime + "ms\n");
            writer.write("  实际吞吐量: " + String.format("%.2f", 
                       (totalMessagesSent.get() * 1000.0 / testDuration)) + " TPS\n\n");
            
            writer.write("系统资源统计:\n");
            writer.write("  平均CPU使用率: " + String.format("%.2f", avgCpuUsage) + "%\n");
            writer.write("  平均堆内存使用: " + String.format("%.2f", avgHeapUsage / (1024 * 1024)) + "MB\n\n");
            
            if (!connectionFailureReasons.isEmpty()) {
                writer.write("连接失败原因分析:\n");
                for (Map.Entry<String, Integer> entry : connectionFailureReasons.entrySet()) {
                    writer.write("  " + entry.getKey() + ": " + entry.getValue() + "次\n");
                }
                writer.write("\n");
            }
            
            writer.write("========================================\n");
            writer.write("报告生成时间: " + sdf.format(new Date()) + "\n");
        }
    }
    
    // 计算百分位值
    private long calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        Collections.sort(values);
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
    
    // 创建会话配置文件
    private String createSessionConfig(int sessionId) throws IOException {
        // 读取模板并替换会话特定参数
        List<String> lines = new ArrayList<>();
        Scanner scanner = new Scanner(new File(configTemplatePath));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // 替换SenderCompID，添加会话ID后缀
            if (line.startsWith("SenderCompID=")) {
                line += "_" + sessionId;
            }
            lines.add(line);
        }
        scanner.close();
        
        // 写入临时配置文件
        String tempConfigPath = "fixconfig_session_" + sessionId + ".cfg";
        try (FileWriter writer = new FileWriter(tempConfigPath)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
        return tempConfigPath;
    }

    public static void main(String[] args) throws Exception {
        // 解析命令行参数（使用commons-cli库实现，简化代码未展示）
        String configTemplatePath = "fixconfig.template";
        int sessionCount = 10;
        int messagesPerSession = 1000;
        int tps = 20;
        int durationMinutes = 0;
        String outputReportPath = "test_report_" + System.currentTimeMillis() + ".txt";
        
        // 实际应用中应使用CLI库解析参数
        FixPressureTester tester = new FixPressureTester(
            configTemplatePath, sessionCount, messagesPerSession, tps, durationMinutes, outputReportPath);
        tester.startTest();
    }
    
    // 系统资源指标内部类
    private static class SystemMetrics {
        final long timestamp;
        final double cpuUsage;
        final long freeMemory;
        final long totalMemory;
        final long heapUsed;
        
        SystemMetrics(long timestamp, double cpuUsage, long freeMemory, long totalMemory, long heapUsed) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.freeMemory = freeMemory;
            this.totalMemory = totalMemory;
            this.heapUsed = heapUsed;
        }
    }
}
    