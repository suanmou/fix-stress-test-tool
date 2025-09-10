package com.financial.fix.stresstest;

import quickfix.ConfigError;
import quickfix.RuntimeError;
import quickfix.SessionSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * FIX压力测试工具主类，包含GCP指标收集功能
 */
public class FixStressTester {
    private final String configFile;
    private final int numClients;
    private final int messagesPerClient;
    private final String gcpProjectId;
    private final String vmInstanceId;
    private final String gcpZone;
    
    private final ExecutorService executor;
    private final List<FixClientTask> clientTasks = new ArrayList<>();
    private Instant testStartTime;
    private Instant testEndTime;

    public FixStressTester(String configFile, int numClients, int messagesPerClient,
                          String gcpProjectId, String vmInstanceId, String gcpZone) {
        this.configFile = configFile;
        this.numClients = numClients;
        this.messagesPerClient = messagesPerClient;
        this.gcpProjectId = gcpProjectId;
        this.vmInstanceId = vmInstanceId;
        this.gcpZone = gcpZone;
        this.executor = Executors.newFixedThreadPool(numClients);
    }

    /**
     * 启动压力测试
     */
    public void startTest() throws Exception {
        System.out.println("Starting FIX stress test with GCP metrics collection...");
        System.out.println("Number of clients: " + numClients);
        System.out.println("Messages per client: " + messagesPerClient);
        System.out.println("Total messages: " + numClients * messagesPerClient);

        // 初始化客户端任务
        for (int i = 0; i < numClients; i++) {
            String senderCompId = "TEST-SENDER-" + i;
            FixClientTask clientTask = new FixClientTask(configFile, senderCompId, messagesPerClient);
            clientTasks.add(clientTask);
        }

        // 开始测试计时
        testStartTime = Instant.now();
        
        // 提交所有客户端任务
        List<Future<Void>> futures = clientTasks.stream()
                .map(executor::submit)
                .collect(Collectors.toList());

        // 等待所有任务完成
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Client task failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 测试结束
        testEndTime = Instant.now();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        // 收集并分析结果
        analyzeResults();
    }

    /**
     * 分析测试结果，包括GCP指标
     */
    private void analyzeResults() throws Exception {
        System.out.println("\n=== Test Results Analysis ===");
        System.out.println("Test started: " + testStartTime);
        System.out.println("Test finished: " + testEndTime);
        
        long totalDurationMs = java.time.Duration.between(testStartTime, testEndTime).toMillis();
        double totalDurationSec = totalDurationMs / 1000.0;
        System.out.println("Total test duration: " + totalDurationMs + " ms (" + 
                          String.format("%.2f", totalDurationSec) + " seconds)");

        // 收集应用层指标
        List<FixTestResult> allResults = new ArrayList<>();
        for (FixClientTask client : clientTasks) {
            allResults.addAll(client.getResults());
        }

        long totalMessages = allResults.size();
        long successfulMessages = allResults.stream().filter(FixTestResult::isSuccess).count();
        double successRate = (double) successfulMessages / totalMessages * 100;
        
        System.out.println("\n=== Application Layer Metrics ===");
        System.out.println("Total messages sent: " + totalMessages);
        System.out.println("Successful messages: " + successfulMessages + " (" + 
                          String.format("%.2f", successRate) + "%)");
        
        // 计算吞吐量
        double throughput = totalMessages / totalDurationSec;
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " messages/second");

        // 计算响应时间统计
        List<Long> responseTimes = allResults.stream()
                .filter(FixTestResult::isSuccess)
                .map(FixTestResult::getResponseTimeMs)
                .collect(Collectors.toList());

        if (!responseTimes.isEmpty()) {
            long minResponse = responseTimes.stream().mapToLong(v -> v).min().orElse(0);
            long maxResponse = responseTimes.stream().mapToLong(v -> v).max().orElse(0);
            double avgResponse = responseTimes.stream().mapToLong(v -> v).average().orElse(0);
            
            System.out.println("\nResponse Time Statistics (ms):");
            System.out.println("Min: " + minResponse);
            System.out.println("Max: " + maxResponse);
            System.out.println("Average: " + String.format("%.2f", avgResponse));
            
            // 计算百分位数
            responseTimes.sort(Long::compareTo);
            System.out.println("P90: " + calculatePercentile(responseTimes, 90));
            System.out.println("P95: " + calculatePercentile(responseTimes, 95));
            System.out.println("P99: " + calculatePercentile(responseTimes, 99));
        }

        // 收集并展示GCP指标
        if (gcpProjectId != null && !gcpProjectId.isEmpty()) {
            System.out.println("\n=== GCP Infrastructure Metrics ===");
            GcpMetricsCollector metricsCollector = new GcpMetricsCollector(gcpProjectId, vmInstanceId, gcpZone);
            Map<String, Double> gcpMetrics = metricsCollector.collectMetrics(testStartTime, testEndTime);
            metricsCollector.close();

            System.out.println("Average CPU usage: " + String.format("%.2f%%", gcpMetrics.get("cpu_usage_average") * 100));
            System.out.println("Network receive rate: " + String.format("%.2f KB/s", 
                              gcpMetrics.get("network_receive_bytes_per_sec") / 1024));
            System.out.println("Network transmit rate: " + String.format("%.2f KB/s", 
                              gcpMetrics.get("network_transmit_bytes_per_sec") / 1024));
            System.out.println("Average network latency: " + String.format("%.2f ms", 
                              gcpMetrics.get("network_latency_ms")));
            System.out.println("Firewall dropped bytes rate: " + String.format("%.2f B/s", 
                              gcpMetrics.get("firewall_dropped_bytes_per_sec")));
        }

        // 错误分析
        long errorCount = totalMessages - successfulMessages;
        if (errorCount > 0) {
            System.out.println("\n=== Error Analysis ===");
            System.out.println("Total errors: " + errorCount);
            
            // 简单的错误分类统计
            Map<String, Long> errorTypes = allResults.stream()
                    .filter(r -> !r.isSuccess())
                    .collect(Collectors.groupingBy(
                            FixTestResult::getErrorMessage,
                            Collectors.counting()
                    ));
            
            errorTypes.forEach((error, count) -> 
                System.out.println("- " + error + ": " + count + " occurrences"));
        }
    }

    /**
     * 计算响应时间百分位数
     */
    private long calculatePercentile(List<Long> sortedTimes, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        return sortedTimes.get(index);
    }

    public static void main(String[] args) {
        try {
            // 读取配置参数
            String configFile = "fix-client.cfg";
            int numClients = 5;
            int messagesPerClient = 100;
            
            // GCP配置 - 从环境变量或命令行参数获取
            String gcpProjectId = System.getenv("GCP_PROJECT_ID");
            String vmInstanceId = System.getenv("GCP_VM_INSTANCE_ID");
            String gcpZone = System.getenv("GCP_ZONE");

            // 从命令行参数覆盖配置
            if (args.length >= 3) {
                numClients = Integer.parseInt(args[0]);
                messagesPerClient = Integer.parseInt(args[1]);
                configFile = args[2];
            }
            
            if (args.length >= 6) {
                gcpProjectId = args[3];
                vmInstanceId = args[4];
                gcpZone = args[5];
            }

            // 启动测试
            FixStressTester tester = new FixStressTester(
                configFile, numClients, messagesPerClient,
                gcpProjectId, vmInstanceId, gcpZone
            );
            tester.startTest();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    