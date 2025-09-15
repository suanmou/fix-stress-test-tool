package com.finance.fix.tester.api;

import com.finance.fix.tester.FixPressureTester;
import com.finance.fix.tester.model.TestReport;
import com.finance.fix.tester.service.TestReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import quickfix.ConfigError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 更新后的测试运行器，集成MongoDB存储测试报告
 */
@Component
public class FixTestRunner implements Runnable {

    private final String taskId;
    private final TestParameters parameters;
    private final String configId;
    private final String configName;
    private final Consumer<Integer> progressCallback;
    private final Consumer<TestReport> completionCallback;
    private volatile boolean isRunning = true;
    private FixPressureTester tester;
    
    // 注入报告服务
    private final TestReportService reportService;

    @Autowired
    public FixTestRunner(String taskId, TestParameters parameters, String configId, String configName,
                         Consumer<Integer> progressCallback, Consumer<TestReport> completionCallback,
                         TestReportService reportService) {
        this.taskId = taskId;
        this.parameters = parameters;
        this.configId = configId;
        this.configName = configName;
        this.progressCallback = progressCallback;
        this.completionCallback = completionCallback;
        this.reportService = reportService;
    }

    @Override
    public void run() {
        // 创建初始报告并保存到MongoDB
        TestReport report = initializeReport();
        reportService.createReport(report);
        
        try {
            // 初始化压力测试器
            tester = new FixPressureTester(parameters.getConfigPath());
            
            // 设置进度回调，定期更新报告
            tester.setProgressListener(progress -> {
                if (isRunning) {
                    progressCallback.accept(progress);
                    // 更新报告进度
                    report.setStatus("RUNNING");
                    updateReportWithCurrentStats(report, progress);
                    reportService.updateReport(report);
                }
            });
            
            // 执行测试
            TestResults results;
            if (parameters.getMessages() != null) {
                // 按消息总数模式运行
                results = tester.runByMessageCount(
                    parameters.getSessions(),
                    parameters.getMessages(),
                    parameters.getRate(),
                    parameters.getTimeout()
                );
            } else {
                // 按持续时间模式运行
                results = tester.runByDuration(
                    parameters.getSessions(),
                    parameters.getDuration(),
                    parameters.getRate(),
                    parameters.getTimeout()
                );
            }
            
            // 测试完成，更新报告
            if (isRunning) {
                report.setStatus("COMPLETED");
                report.setEndTime(LocalDateTime.now());
                populateReportWithResults(report, results);
                reportService.updateReport(report);
                completionCallback.accept(report);
            }
        } catch (ConfigError e) {
            // 配置错误处理
            report.setStatus("FAILED");
            report.setEndTime(LocalDateTime.now());
            report.setErrorMessage("配置错误: " + e.getMessage());
            reportService.updateReport(report);
            completionCallback.accept(report);
        } catch (Exception e) {
            // 其他异常处理
            report.setStatus("FAILED");
            report.setEndTime(LocalDateTime.now());
            report.setErrorMessage("测试执行失败: " + e.getMessage());
            reportService.updateReport(report);
            completionCallback.accept(report);
        }
    }

    /**
     * 初始化测试报告
     */
    private TestReport initializeReport() {
        TestReport report = new TestReport();
        report.setId(taskId);
        report.setConfigId(configId);
        report.setConfigName(configName);
        report.setStartTime(LocalDateTime.now());
        report.setStatus("RUNNING");
        
        // 设置测试参数
        report.setTotalSessions(parameters.getSessions());
        report.setTargetRate(parameters.getRate());
        report.setTotalMessages(parameters.getMessages());
        report.setDurationMinutes(parameters.getDuration());
        report.setTimeoutSeconds(parameters.getTimeout());
        
        // 初始化系统指标存储
        report.setSystemMetrics(new HashMap<>());
        report.getSystemMetrics().put("cpu", new ArrayList<>());
        report.getSystemMetrics().put("memory", new ArrayList<>());
        report.getSystemMetrics().put("networkSend", new ArrayList<>());
        report.getSystemMetrics().put("networkReceive", new ArrayList<>());
        
        return report;
    }

    /**
     * 用当前统计信息更新报告
     */
    private void updateReportWithCurrentStats(TestReport report, int progress) {
        // 更新基本统计信息
        report.setTotalMessagesSent(tester.getSentCount());
        report.setTotalResponsesReceived(tester.getReceivedCount());
        report.setTimeoutCount(tester.getTimeoutCount());
        
        // 计算当前TPS
        long elapsedSeconds = java.time.Duration.between(
            report.getStartTime(), LocalDateTime.now()
        ).getSeconds();
        
        if (elapsedSeconds > 0) {
            report.setActualRate((double) tester.getSentCount() / elapsedSeconds);
        }
        
        // 记录系统指标(每30秒记录一次)
        if (tester.getSystemMetrics() != null && 
            System.currentTimeMillis() % 30000 < 1000) { // 每30秒左右记录一次
            recordSystemMetrics(report);
        }
    }

    /**
     * 记录系统指标到报告
     */
    private void recordSystemMetrics(TestReport report) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Double> metrics = tester.getSystemMetrics();
        
        if (metrics == null) return;
        
        // 记录CPU使用率
        if (metrics.containsKey("cpu")) {
            report.getSystemMetrics().get("cpu").add(
                new TestReport.SystemMetric(now, metrics.get("cpu"), "%")
            );
        }
        
        // 记录内存使用率
        if (metrics.containsKey("memory")) {
            report.getSystemMetrics().get("memory").add(
                new TestReport.SystemMetric(now, metrics.get("memory"), "%")
            );
        }
        
        // 记录网络发送速率
        if (metrics.containsKey("networkSend")) {
            report.getSystemMetrics().get("networkSend").add(
                new TestReport.SystemMetric(now, metrics.get("networkSend"), "Kbps")
            );
        }
        
        // 记录网络接收速率
        if (metrics.containsKey("networkReceive")) {
            report.getSystemMetrics().get("networkReceive").add(
                new TestReport.SystemMetric(now, metrics.get("networkReceive"), "Kbps")
            );
        }
    }

    /**
     * 用测试结果填充报告
     */
    private void populateReportWithResults(TestReport report, TestResults results) {
        // 填充基本统计
        report.setTotalMessagesSent(results.getTotalMessagesSent());
        report.setTotalResponsesReceived(results.getTotalResponsesReceived());
        report.setTimeoutCount(results.getTimeoutCount());
        report.setActualRate(results.getActualThroughput());
        
        // 填充响应时间统计
        report.setAverageResponseTime(results.getAverageResponseTime());
        report.setP95ResponseTime(results.getP95ResponseTime());
        report.setP99ResponseTime(results.getP99ResponseTime());
        
        // 填充连接统计
        report.setConnectionSuccessRate(results.getConnectionSuccessRate());
        report.setAverageConnectionTime(results.getAverageConnectionTime());
        
        // 填充连接错误信息
        List<TestReport.ConnectionError> connectionErrors = new ArrayList<>();
        results.getConnectionErrors().forEach((type, error) -> {
            connectionErrors.add(new TestReport.ConnectionError(
                type, error.getCount(), error.getMessage()
            ));
        });
        report.setConnectionErrors(connectionErrors);
        
        // 记录最后一次系统指标
        recordSystemMetrics(report);
    }

    /**
     * 停止当前测试
     */
    public void stop() {
        isRunning = false;
        if (tester != null) {
            tester.stop();
        }
        
        // 更新报告状态为已停止
        reportService.getReportById(taskId).ifPresent(report -> {
            report.setStatus("STOPPED");
            report.setEndTime(LocalDateTime.now());
            updateReportWithCurrentStats(report, 0);
            reportService.updateReport(report);
        });
    }
}
    