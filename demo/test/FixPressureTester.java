package com.finance.fix.tester;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.Heartbeat;
import quickfix.fix44.TestRequest;
import org.apache.commons.cli.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FixPressureTester {
    private final String configTemplatePath;
    private final int numSessions;
    private final int messagesPerSession;
    private final int messagesPerSecond;
    private final int durationMinutes;
    private final long timeoutMillis = 5000; // 5秒超时
    
    // 全局统计
    private final AtomicInteger totalSent = new AtomicInteger(0);
    private final AtomicInteger totalReceived = new AtomicInteger(0);
    private final AtomicInteger totalTimeout = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();
    
    public FixPressureTester(String configTemplatePath, int numSessions, 
                            int messagesPerSession, int messagesPerSecond, int durationMinutes) {
        this.configTemplatePath = configTemplatePath;
        this.numSessions = numSessions;
        this.messagesPerSession = messagesPerSession;
        this.messagesPerSecond = messagesPerSecond;
        this.durationMinutes = durationMinutes;
    }
    
    public void run() throws Exception {
        System.out.println("开始FIX压力测试...");
        System.out.println("会话数: " + numSessions);
        System.out.println("每个会话消息数: " + (messagesPerSession > 0 ? messagesPerSession : "无限制(按时间)"));
        System.out.println("每秒消息数: " + messagesPerSecond);
        System.out.println("测试时长: " + (durationMinutes > 0 ? durationMinutes + "分钟" : "无限制(按消息数)"));
        
        // 创建会话管理器列表
        List<SessionManager> sessionManagers = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numSessions);
        
        // 启动所有会话
        for (int i = 0; i < numSessions; i++) {
            SessionManager manager = new SessionManager(
                createSessionConfig(i), 
                messagesPerSession,
                messagesPerSecond,
                durationMinutes,
                timeoutMillis,
                this::onResponseReceived
            );
            sessionManagers.add(manager);
            executor.submit(manager);
        }
        
        // 等待所有会话完成
        executor.shutdown();
        executor.awaitTermination(durationMinutes > 0 ? durationMinutes + 5 : 60, TimeUnit.MINUTES);
        
        // 检查超时的请求
        checkTimeouts(sessionManagers);
        
        // 生成测试报告
        generateReport();
    }
    
    private SessionSettings createSessionConfig(int sessionId) throws Exception {
        // 基于模板创建会话配置，为每个会话设置唯一ID
        SessionSettings baseSettings = new SessionSettings(new File(configTemplatePath));
        SessionSettings sessionSettings = new SessionSettings();
        
        // 复制基础配置并修改会话ID
        for (Dictionary dict : baseSettings) {
            Dictionary newDict = new Dictionary();
            newDict.putAll(dict);
            newDict.setString(SessionID.BEGINSTRING, dict.getString(SessionID.BEGINSTRING));
            newDict.setString(SessionID.SENDERCOMPID, dict.getString(SessionID.SENDERCOMPID) + "_" + sessionId);
            newDict.setString(SessionID.TARGETCOMPID, dict.getString(SessionID.TARGETCOMPID));
            sessionSettings.set(new SessionID(newDict), newDict);
        }
        
        return sessionSettings;
    }
    
    private void onResponseReceived(long responseTime) {
        totalReceived.incrementAndGet();
        responseTimes.add(responseTime);
    }
    
    private void checkTimeouts(List<SessionManager> sessionManagers) {
        int timeoutCount = 0;
        for (SessionManager manager : sessionManagers) {
            timeoutCount += manager.getTimeoutCount();
        }
        totalTimeout.set(timeoutCount);
    }
    
    private void generateReport() {
        System.out.println("\n===== 测试报告 =====");
        System.out.println("总发送消息数: " + totalSent.get());
        System.out.println("总接收响应数: " + totalReceived.get());
        System.out.println("超时消息数: " + totalTimeout.get());
        System.out.println("响应率: " + String.format("%.2f%%", 
            totalSent.get() > 0 ? (double)totalReceived.get() / totalSent.get() * 100 : 0));
        
        if (!responseTimes.isEmpty()) {
            List<Long> sortedTimes = responseTimes.stream().sorted().collect(Collectors.toList());
            long min = sortedTimes.get(0);
            long max = sortedTimes.get(sortedTimes.size() - 1);
            long avg = (long) sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            
            int p95Index = (int) (sortedTimes.size() * 0.95);
            long p95 = sortedTimes.get(p95Index);
            
            int p99Index = (int) (sortedTimes.size() * 0.99);
            long p99 = sortedTimes.get(p99Index);
            
            System.out.println("\n响应时间统计 (毫秒):");
            System.out.println("最小值: " + min);
            System.out.println("最大值: " + max);
            System.out.println("平均值: " + avg);
            System.out.println("95%分位值: " + p95);
            System.out.println("99%分位值: " + p99);
        }
        System.out.println("====================");
    }
    
    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        Options options = new Options();
        options.addOption("config", true, "配置文件模板路径");
        options.addOption("sessions", true, "会话数量");
        options.addOption("messages", true, "每个会话的消息总数");
        options.addOption("rate", true, "每秒消息数");
        options.addOption("duration", true, "测试持续时间(分钟)");
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        // 验证必要参数
        if (!cmd.hasOption("config") || !cmd.hasOption("sessions") || 
            !cmd.hasOption("rate") || !(cmd.hasOption("messages") || cmd.hasOption("duration"))) {
            System.err.println("使用方法: FixPressureTester -config <配置模板> -sessions <数量> -rate <每秒消息数> " +
                             "(-messages <总数> | -duration <分钟>)");
            System.exit(1);
        }
        
        // 创建并运行测试器
        new FixPressureTester(
            cmd.getOptionValue("config"),
            Integer.parseInt(cmd.getOptionValue("sessions")),
            cmd.hasOption("messages") ? Integer.parseInt(cmd.getOptionValue("messages")) : 0,
            Integer.parseInt(cmd.getOptionValue("rate")),
            cmd.hasOption("duration") ? Integer.parseInt(cmd.getOptionValue("duration")) : 0
        ).run();
    }
}
    