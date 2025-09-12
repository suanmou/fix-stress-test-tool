package com.finance.fix.tester.api;

import com.finance.fix.tester.FixPressureTester;
import quickfix.ConfigError;

import java.util.function.Consumer;

/**
 * 测试运行器线程类
 * 负责实际执行压力测试逻辑
 */
public class FixTestRunner implements Runnable {

    private final String taskId;
    private final TestParameters parameters;
    private final Consumer<Integer> progressCallback;
    private final Consumer<TestReport> completionCallback;
    private volatile boolean isRunning = true;
    private FixPressureTester tester;

    public FixTestRunner(String taskId, TestParameters parameters,
                         Consumer<Integer> progressCallback,
                         Consumer<TestReport> completionCallback) {
        this.taskId = taskId;
        this.parameters = parameters;
        this.progressCallback = progressCallback;
        this.completionCallback = completionCallback;
    }

    @Override
    public void run() {
        try {
            // 初始化压力测试器
            tester = new FixPressureTester(parameters.getConfigPath());
            
            // 设置进度回调
            tester.setProgressListener(progress -> {
                if (isRunning) {
                    progressCallback.accept(progress);
                }
            });
            
            // 执行测试
            if (parameters.getMessages() != null) {
                // 按消息总数模式运行
                tester.runByMessageCount(
                    parameters.getSessions(),
                    parameters.getMessages(),
                    parameters.getRate(),
                    parameters.getTimeout()
                );
            } else {
                // 按持续时间模式运行
                tester.runByDuration(
                    parameters.getSessions(),
                    parameters.getDuration(),
                    parameters.getRate(),
                    parameters.getTimeout()
                );
            }
            
            // 测试完成，生成报告
            if (isRunning) {
                TestReport report = convertToTestReport(tester.getTestResults());
                completionCallback.accept(report);
            }
        } catch (ConfigError e) {
            // 配置错误处理
            TestReport errorReport = new TestReport();
            errorReport.setTaskId(taskId);
            errorReport.setStatus("FAILED");
            errorReport.setErrorMessage("配置错误: " + e.getMessage());
            completionCallback.accept(errorReport);
        } catch (Exception e) {
            // 其他异常处理
            TestReport errorReport = new TestReport();
            errorReport.setTaskId(taskId);
            errorReport.setStatus("FAILED");
            errorReport.setErrorMessage("测试执行失败: " + e.getMessage());
            completionCallback.accept(errorReport);
        }
    }

    /**
     * 停止当前测试
     */
    public void stop() {
        isRunning = false;
        if (tester != null) {
            tester.stop();
        }
    }

    /**
     * 将测试结果转换为API报告格式
     */
    private TestReport convertToTestReport(TestResults results) {
        TestReport report = new TestReport();
        report.setTaskId(taskId);
        report.setStatus("COMPLETED");
        report.setStartTime(results.getStartTime());
        report.setEndTime(results.getEndTime());
        report.setTotalSessions(parameters.getSessions());
        report.setTargetRate(parameters.getRate());
        report.setActualRate(results.getActualThroughput());
        report.setTotalMessagesSent(results.getTotalMessagesSent());
        report.setTotalResponsesReceived(results.getTotalResponsesReceived());
        report.setTimeoutCount(results.getTimeoutCount());
        report.setConnectionSuccessRate(results.getConnectionSuccessRate());
        report.setAverageConnectionTime(results.getAverageConnectionTime());
        report.setAverageResponseTime(results.getAverageResponseTime());
        report.setP95ResponseTime(results.getP95ResponseTime());
        report.setP99ResponseTime(results.getP99ResponseTime());
        report.setConnectionErrors(results.getConnectionErrors());
        return report;
    }
}
    