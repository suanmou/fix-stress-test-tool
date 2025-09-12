package com.finance.fix.tester.api;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FixTestService {

    // 存储所有测试任务的状态
    private final Map<String, TestStatusResponse> testStatusMap = new ConcurrentHashMap<>();
    
    // 存储所有测试报告
    private final Map<String, TestReport> testReports = new ConcurrentHashMap<>();
    
    // 存储运行中的测试任务，用于终止操作
    private final Map<String, FixTestRunner> runningTests = new ConcurrentHashMap<>();
    
    // 线程池用于执行测试任务
    private final ExecutorService testExecutor = Executors.newCachedThreadPool();

    /**
     * 启动新的压力测试
     */
    public void startTest(String taskId, TestParameters parameters) {
        // 初始化任务状态
        TestStatusResponse initialStatus = new TestStatusResponse();
        initialStatus.setTaskId(taskId);
        initialStatus.setStatus("RUNNING");
        initialStatus.setProgress(0);
        initialStatus.setStartTime(System.currentTimeMillis());
        testStatusMap.put(taskId, initialStatus);
        
        // 创建测试运行器
        FixTestRunner testRunner = new FixTestRunner(taskId, parameters, 
            progress -> {
                // 更新进度回调
                TestStatusResponse status = testStatusMap.get(taskId);
                if (status != null) {
                    status.setProgress(progress);
                }
            },
            report -> {
                // 测试完成回调
                testReports.put(taskId, report);
                TestStatusResponse status = testStatusMap.get(taskId);
                if (status != null) {
                    status.setStatus("COMPLETED");
                    status.setEndTime(System.currentTimeMillis());
                    status.setProgress(100);
                }
                runningTests.remove(taskId);
            }
        );
        
        // 存储并提交任务
        runningTests.put(taskId, testRunner);
        testExecutor.submit(testRunner);
    }

    /**
     * 获取测试任务状态
     */
    public TestStatusResponse getTestStatus(String taskId) {
        return testStatusMap.get(taskId);
    }

    /**
     * 获取测试报告
     */
    public TestReport getTestReport(String taskId) {
        return testReports.get(taskId);
    }

    /**
     * 终止测试任务
     */
    public boolean stopTest(String taskId) {
        FixTestRunner testRunner = runningTests.get(taskId);
        if (testRunner != null) {
            testRunner.stop();
            
            // 更新状态
            TestStatusResponse status = testStatusMap.get(taskId);
            if (status != null) {
                status.setStatus("STOPPED");
                status.setEndTime(System.currentTimeMillis());
            }
            
            runningTests.remove(taskId);
            return true;
        }
        return false;
    }
}
    