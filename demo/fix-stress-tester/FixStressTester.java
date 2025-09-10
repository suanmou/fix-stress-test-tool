import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FixStressTester {
    // 测试配置参数
    private final String fixConfigPath;
    private final int concurrency;
    private final int totalMessages;
    private final String senderCompIdPrefix;
    private final String targetCompId;
    
    // 测试结果记录
    private final List<FixTestResult> results = new CopyOnWriteArrayList<>();
    private Instant testStartTime;
    private Instant testEndTime;

    public FixStressTester(String fixConfigPath, int concurrency, int totalMessages,
                          String senderCompIdPrefix, String targetCompId) {
        this.fixConfigPath = fixConfigPath;
        this.concurrency = concurrency;
        this.totalMessages = totalMessages;
        this.senderCompIdPrefix = senderCompIdPrefix;
        this.targetCompId = targetCompId;
    }

    public void runTest() throws Exception {
        System.out.println("开始FIX压力测试...");
        System.out.println("目标服务器: " + targetCompId);
        System.out.println("并发客户端数: " + concurrency);
        System.out.println("总消息数: " + totalMessages);

        testStartTime = Instant.now();
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        // 每个客户端需要发送的消息数
        int messagesPerClient = totalMessages / concurrency;
        int remainingMessages = totalMessages % concurrency;

        // 创建所有测试任务
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (int i = 0; i < concurrency; i++) {
            int clientMessages = messagesPerClient + (i < remainingMessages ? 1 : 0);
            String senderCompId = senderCompIdPrefix + "-" + (i + 1);
            
            tasks.add(new FixClientTask(fixConfigPath, senderCompId, targetCompId, 
                                      clientMessages, results));
        }

        try {
            // 执行所有任务
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("测试被中断: " + e.getMessage());
        } finally {
            executor.shutdown();
            testEndTime = Instant.now();
        }

        System.out.println("FIX压力测试完成!");
    }

    // 分析并展示测试结果
    public void analyzeResults() {
        if (results.isEmpty()) {
            System.out.println("没有测试结果可分析");
            return;
        }

        long totalTimeMs = Duration.between(testStartTime, testEndTime).toMillis();
        long totalSuccessful = results.stream().filter(r -> r.isSuccess()).count();
        double successRate = (double) totalSuccessful / results.size() * 100;
        
        // 响应时间统计
        List<Long> responseTimes = results.stream()
                .filter(r -> r.isSuccess())
                .map(r -> r.getResponseTimeMs())
                .collect(Collectors.toList());
        
        long minResponseTime = responseTimes.stream().mapToLong(v -> v).min().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(v -> v).max().orElse(0);
        long avgResponseTime = responseTimes.isEmpty() ? 0 : 
                (long) responseTimes.stream().mapToLong(v -> v).average().orElse(0);
        
        // 吞吐量 (messages/second)
        double throughput = totalTimeMs > 0 ? (double) results.size() / (totalTimeMs / 1000.0) : 0;

        // 错误分布
        Map<String, Long> errorDistribution = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.groupingBy(
                        r -> r.getErrorMessage() != null ? r.getErrorMessage() : "Unknown error",
                        Collectors.counting()
                ));

        // 输出分析结果
        System.out.println("\n===== 测试结果分析 =====");
        System.out.println("总消息数: " + results.size());
        System.out.println("成功消息数: " + totalSuccessful);
        System.out.println("成功率: " + String.format("%.2f", successRate) + "%");
        System.out.println("总测试时间: " + totalTimeMs + "ms");
        System.out.println("吞吐量: " + String.format("%.2f", throughput) + " messages/second");
        
        if (!responseTimes.isEmpty()) {
            System.out.println("\n响应时间统计:");
            System.out.println("最小响应时间: " + minResponseTime + "ms");
            System.out.println("最大响应时间: " + maxResponseTime + "ms");
            System.out.println("平均响应时间: " + avgResponseTime + "ms");
            
            // 计算百分位数
            calculatePercentiles(responseTimes);
        }
        
        if (!errorDistribution.isEmpty()) {
            System.out.println("\n错误分布:");
            errorDistribution.forEach((error, count) -> 
                System.out.println("  " + error + ": " + count + "次"));
        }
    }
    
    // 计算并输出响应时间百分位数
    private void calculatePercentiles(List<Long> responseTimes) {
        Collections.sort(responseTimes);
        int size = responseTimes.size();
        
        long p90 = responseTimes.get((int) Math.ceil(size * 0.9) - 1);
        long p95 = responseTimes.get((int) Math.ceil(size * 0.95) - 1);
        long p99 = responseTimes.get((int) Math.ceil(size * 0.99) - 1);
        
        System.out.println("响应时间百分位:");
        System.out.println("  P90: " + p90 + "ms");
        System.out.println("  P95: " + p95 + "ms");
        System.out.println("  P99: " + p99 + "ms");
    }

    public static void main(String[] args) {
        try {
            // 默认配置
            String fixConfigPath = "fix-client.cfg";
            int concurrency = 5;
            int totalMessages = 1000;
            String senderCompIdPrefix = "TEST-SENDER";
            String targetCompId = "TEST-TARGET";

            // 可以从命令行参数读取配置
            if (args.length >= 5) {
                fixConfigPath = args[0];
                concurrency = Integer.parseInt(args[1]);
                totalMessages = Integer.parseInt(args[2]);
                senderCompIdPrefix = args[3];
                targetCompId = args[4];
            }

            FixStressTester tester = new FixStressTester(
                    fixConfigPath, concurrency, totalMessages, senderCompIdPrefix, targetCompId);
            
            tester.runTest();
            tester.analyzeResults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    