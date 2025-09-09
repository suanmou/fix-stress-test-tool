package com.fix.test.service;

import com.fix.test.entity.TestPlan;
import com.fix.test.entity.TestTask;
import com.fix.test.repository.TestPlanRepository;
import com.fix.test.repository.TestTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TestTaskScheduler {
    
    @Autowired
    private TestPlanRepository testPlanRepository;
    
    @Autowired
    private TestTaskRepository testTaskRepository;
    
    @Autowired
    private FixPressureService fixPressureService;
    
    @Autowired
    private MonitorDataCollector monitorDataCollector;
    
    /**
     * 启动测试计划
     */
    @Transactional
    public TestTask startTestPlan(Long planId, String startType, String emergencyToken) throws Exception {
        TestPlan plan = testPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("测试计划不存在"));
        
        // 检查计划状态
        if (plan.getStatus() != TestPlan.PlanStatus.CONFIGURED) {
            throw new IllegalStateException("测试计划状态不正确");
        }
        
        // 创建任务
        TestTask task = new TestTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTestPlan(plan);
        task.setEmergencyToken(emergencyToken);
        task.setStatus(TestTask.TaskStatus.STARTING);
        task.setStartTime(LocalDateTime.now());
        
        task = testTaskRepository.save(task);
        
        // 异步启动测试
        new Thread(() -> {
            try {
                executeTest(task);
            } catch (Exception e) {
                handleTestFailure(task, e);
            }
        }).start();
        
        return task;
    }
    
    /**
     * 执行测试
     */
    private void executeTest(TestTask task) throws Exception {
        TestPlan plan = task.getTestPlan();
        
        try {
            // 初始化会话
            fixPressureService.initializeSessions(task.getTaskId(), plan);
            
            // 更新状态为运行中
            updateTaskStatus(task.getTaskId(), TestTask.TaskStatus.RUNNING);
            
            // 开始监控数据采集
            monitorDataCollector.startCollection(task.getTaskId());
            
            // 开始压力测试
            fixPressureService.startPressureTest(task.getTaskId(), plan);
            
        } catch (Exception e) {
            handleTestFailure(task, e);
            throw e;
        }
    }
    
    /**
     * 暂停测试
     */
    public void pauseTest(String taskId) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        if (task.getStatus() != TestTask.TaskStatus.RUNNING) {
            throw new IllegalStateException("任务状态不允许暂停");
        }
        
        fixPressureService.pauseTest(taskId);
        updateTaskStatus(taskId, TestTask.TaskStatus.PAUSED);
    }
    
    /**
     * 恢复测试
     */
    public void resumeTest(String taskId) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        if (task.getStatus() != TestTask.TaskStatus.PAUSED) {
            throw new IllegalStateException("任务状态不允许恢复");
        }
        
        fixPressureService.resumeTest(taskId);
        updateTaskStatus(taskId, TestTask.TaskStatus.RUNNING);
    }
    
    /**
     * 停止测试
     */
    public void stopTest(String taskId, String emergencyToken) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        if (!task.getEmergencyToken().equals(emergencyToken)) {
            throw new IllegalArgumentException("紧急停止令牌不正确");
        }
        
        fixPressureService.stopTest(taskId);
        monitorDataCollector.stopCollection(taskId);
        
        task.setEndTime(LocalDateTime.now());
        task.setTotalDuration(
            java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMillis()
        );
        
        updateTaskStatus(taskId, TestTask.TaskStatus.COMPLETED);
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, TestTask.TaskStatus status) {
        testTaskRepository.updateStatus(taskId, status);
    }
    
    /**
     * 处理测试失败
     */
    private void handleTestFailure(TestTask task, Exception e) {
        task.setStatus(TestTask.TaskStatus.FAILED);
        task.setErrorMessage(e.getMessage());
        task.setEndTime(LocalDateTime.now());
        testTaskRepository.save(task);
    }
    
    /**
     * 获取任务列表
     */
    public List<TestTask> getTaskList() {
        return testTaskRepository.findAll();
    }
    
    /**
     * 获取任务详情
     */
    public TestTask getTaskDetail(String taskId) {
        return testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
    }
}