package com.fix.test.service;

import com.fix.test.dto.*;
import com.fix.test.entity.*;
import com.fix.test.repository.TestTaskRepository;
import com.fix.test.repository.TaskStepProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TestTaskService {
    
    @Autowired
    private TestTaskRepository testTaskRepository;
    
    @Autowired
    private TaskStepProgressRepository stepProgressRepository;
    
    @Autowired
    private TestPlanRepository testPlanRepository;
    
    @Autowired
    private FixPressureService fixPressureService;
    
    @Autowired
    private MonitorDataCollector monitorDataCollector;
    
    private final Map<String, TaskExecutionContext> activeContexts = new ConcurrentHashMap<>();
    
    public static class TaskExecutionContext {
        private final String taskId;
        private final TestTask task;
        private volatile boolean running = true;
        private volatile boolean paused = false;
        private final AtomicInteger currentStep = new AtomicInteger(0);
        private final AtomicLong totalMessagesSent = new AtomicLong(0);
        private final AtomicLong totalMessagesReceived = new AtomicLong(0);
        private final AtomicLong totalMessagesFailed = new AtomicLong(0);
        
        public TaskExecutionContext(String taskId, TestTask task) {
            this.taskId = taskId;
            this.task = task;
        }
    }
    
    /**
     * 创建测试任务
     */
    @Transactional
    public TestTask createTestTask(Long planId, CreateTestTaskRequest request) {
        TestPlan plan = testPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("测试计划不存在"));
        
        // 检查计划状态
        if (plan.getStatus() != TestPlan.PlanStatus.CONFIGURED) {
            throw new IllegalStateException("测试计划状态不正确");
        }
        
        // 创建任务
        TestTask task = new TestTask();
        task.setTaskId(generateTaskId());
        task.setTestPlan(plan);
        task.setEmergencyToken(UUID.randomUUID().toString());
        task.setTotalSteps(plan.getTpsSteps().size());
        task.setStatus(TestTask.TaskStatus.STARTING);
        
        // 创建步骤进度
        List<TaskStepProgress> stepProgress = new ArrayList<>();
        for (int i = 0; i < plan.getTpsSteps().size(); i++) {
            TestPlan.TpsStep step = plan.getTpsSteps().get(i);
            TaskStepProgress progress = new TaskStepProgress();
            progress.setTestTask(task);
            progress.setStepNumber(i + 1);
            progress.setTargetTps(step.getTps());
            progress.setStatus(TaskStepProgress.StepStatus.PENDING);
            stepProgress.add(progress);
        }
        
        task.setStepProgress(stepProgress);
        
        return testTaskRepository.save(task);
    }
    
    /**
     * 启动测试任务
     */
    @Transactional
    public void startTask(String taskId) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        if (task.getStatus() != TestTask.TaskStatus.STARTING) {
            throw new IllegalStateException("任务状态不正确");
        }
        
        // 更新状态
        task.setStatus(TestTask.TaskStatus.RUNNING);
        task.setStartTime(LocalDateTime.now());
        testTaskRepository.save(task);
        
        // 启动执行
        TaskExecutionContext context = new TaskExecutionContext(taskId, task);
        activeContexts.put(taskId, context);
        
        // 异步执行
        new Thread(() -> executeTask(taskId, context)).start();
    }
    
    /**
     * 执行任务
     */
    private void executeTask(String taskId, TaskExecutionContext context) {
        TestTask task = context.task;
        TestPlan plan = task.getTestPlan();
        
        try {
            // 初始化会话
            fixPressureService.initializeSessions(taskId, plan);
            
            // 执行每个步骤
            List<TestPlan.TpsStep> steps = plan.getTpsSteps();
            for (int i = 0; i < steps.size(); i++) {
                if (!context.running || context.paused) break;
                
                TestPlan.TpsStep step = steps.get(i);
                executeTaskStep(taskId, i + 1, step, context);
            }
            
            // 任务完成
            completeTask(taskId);
            
        } catch (Exception e) {
            failTask(taskId, e.getMessage());
        }
    }
    
    /**
     * 执行单个步骤
     */
    private void executeTaskStep(String taskId, int stepNumber, TestPlan.TpsStep step, TaskExecutionContext context) {
        TestTask task = context.task;
        TaskStepProgress progress = stepProgressRepository.findByTaskAndStepNumber(task, stepNumber);
        
        progress.setStatus(TaskStepProgress.StepStatus.RUNNING);
        progress.setStartTime(LocalDateTime.now());
        stepProgressRepository.save(progress);
        
        // 更新当前步骤
        task.setCurrentStep(stepNumber);
        task.setCurrentTps(step.getTps());
        testTaskRepository.save(task);
        
        // 执行步骤
        long stepStartTime = System.currentTimeMillis();
        long stepDuration = step.getDuration() * 60 * 1000L; // 转换为毫秒
        
        long messagesPerSecond = step.getTps();
        long interval = 1000L / messagesPerSecond;
        
        while (System.currentTimeMillis() - stepStartTime < stepDuration && context.running) {
            if (context.paused) {
                try {
                    Thread.sleep(1000);
                    continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 发送消息
            sendMessages(taskId, step, context);
            
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 步骤完成
        progress.setEndTime(LocalDateTime.now());
        progress.setDuration(System.currentTimeMillis() - stepStartTime);
        progress.setStatus(TaskStepProgress.StepStatus.COMPLETED);
        stepProgressRepository.save(progress);
    }
    
    /**
     * 发送消息
     */
    private void sendMessages(String taskId, TestPlan.TpsStep step, TaskExecutionContext context) {
        TestPlan plan = context.task.getTestPlan();
        
        // 根据消息配置发送
        for (MessageConfig config : plan.getMessageConfigs()) {
            int messageCount = (int) (step.getTps() * config.getMsgRatio() / 100.0);
            
            for (int i = 0; i < messageCount; i++) {
                try {
                    fixPressureService.sendMessage(taskId, config);
                    context.totalMessagesSent.incrementAndGet();
                } catch (Exception e) {
                    context.totalMessagesFailed.incrementAndGet();
                }
            }
        }
    }
    
    /**
     * 暂停任务
     */
    @Transactional
    public void pauseTask(String taskId) {
        TaskExecutionContext context = activeContexts.get(taskId);
        if (context != null) {
            context.paused = true;
            
            TestTask task = testTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
            task.setStatus(TestTask.TaskStatus.PAUSED);
            testTaskRepository.save(task);
        }
    }
    
    /**
     * 恢复任务
     */
    @Transactional
    public void resumeTask(String taskId) {
        TaskExecutionContext context = activeContexts.get(taskId);
        if (context != null) {
            context.paused = false;
            
            TestTask task = testTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
            task.setStatus(TestTask.TaskStatus.RUNNING);
            testTaskRepository.save(task);
        }
    }
    
    /**
     * 停止任务
     */
    @Transactional
    public void stopTask(String taskId, String emergencyToken) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        if (!task.getEmergencyToken().equals(emergencyToken)) {
            throw new IllegalArgumentException("紧急停止令牌不正确");
        }
        
        TaskExecutionContext context = activeContexts.get(taskId);
        if (context != null) {
            context.running = false;
            activeContexts.remove(taskId);
        }
        
        task.setStatus(TestTask.TaskStatus.STOPPING);
        task.setEndTime(LocalDateTime.now());
        task.setTotalDuration(
            java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMillis()
        );
        testTaskRepository.save(task);
    }
    
    /**
     * 获取任务详情
     */
    public TestTaskDetail getTaskDetail(String taskId) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        TestTaskDetail detail = new TestTaskDetail();
        detail.setTask(task);
        detail.setCurrentMetrics(getCurrentMetrics(taskId));
        
        return detail;
    }
    
    /**
     * 获取任务列表
     */
    public List<TestTaskSummary> getTaskList() {
        return testTaskRepository.findAllSummaries();
    }
    
    /**
     * 获取当前指标
     */
    private TaskMetrics getCurrentMetrics(String taskId) {
        TaskExecutionContext context = activeContexts.get(taskId);
        if (context == null) return new TaskMetrics();
        
        TestTask task = context.task;
        
        TaskMetrics metrics = new TaskMetrics();
        metrics.setTotalMessagesSent(context.totalMessagesSent.get());
        metrics.setTotalMessagesReceived(context.totalMessagesReceived.get());
        metrics.setTotalMessagesFailed(context.totalMessagesFailed.get());
        metrics.setCurrentStep(context.currentStep.get());
        metrics.setCurrentTps(task.getCurrentTps());
        metrics.setProgress(calculateProgress(task));
        
        return metrics;
    }
    
    /**
     * 计算进度
     */
    private double calculateProgress(TestTask task) {
        if (task.getTotalSteps() == 0) return 0.0;
        return (double) task.getCurrentStep() / task.getTotalSteps() * 100;
    }
    
    /**
     * 任务完成
     */
    private void completeTask(String taskId) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        task.setStatus(TestTask.TaskStatus.COMPLETED);
        task.setEndTime(LocalDateTime.now());
        task.setTotalDuration(
            java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMillis()
        );
        testTaskRepository.save(task);
        
        activeContexts.remove(taskId);
    }
    
    /**
     * 任务失败
     */
    private void failTask(String taskId, String errorMessage) {
        TestTask task = testTaskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("测试任务不存在"));
        
        task.setStatus(TestTask.TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        testTaskRepository.save(task);
        
        activeContexts.remove(taskId);
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}