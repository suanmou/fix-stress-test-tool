package com.fix.test.controller;

import com.fix.test.dto.*;
import com.fix.test.entity.TestPlan;
import com.fix.test.entity.TestTask;
import com.fix.test.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {
    
    @Autowired
    private TestPlanService testPlanService;
    
    @Autowired
    private TestTaskScheduler testTaskScheduler;
    
    @Autowired
    private FixPressureService fixPressureService;
    
    @Autowired
    private MonitorDataCollector monitorDataCollector;
    
    /**
     * 创建测试计划
     */
    @PostMapping("/test-plan")
    public ResponseEntity<ApiResponse<TestPlan>> createTestPlan(@Valid @RequestBody CreateTestPlanRequest request) {
        TestPlan plan = testPlanService.createTestPlan(request);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }
    
    /**
     * 获取测试计划详情
     */
    @GetMapping("/test-plan/{planId}")
    public ResponseEntity<ApiResponse<TestPlan>> getTestPlan(@PathVariable Long planId) {
        TestPlan plan = testPlanService.getTestPlan(planId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }
    
    /**
     * 获取测试计划列表
     */
    @GetMapping("/test-plan")
    public ResponseEntity<ApiResponse<List<TestPlan>>> getTestPlans() {
        List<TestPlan> plans = testPlanService.getAllTestPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }
    
    /**
     * 启动测试计划
     */
    @PostMapping("/test-plan/{planId}/start")
    public ResponseEntity<ApiResponse<TestTask>> startTestPlan(
            @PathVariable Long planId,
            @RequestBody StartTestPlanRequest request) {
        
        TestTask task = testTaskScheduler.startTestPlan(
            planId, 
            request.getStartType(), 
            request.getEmergencyToken()
        );
        
        return ResponseEntity.ok(ApiResponse.success(task));
    }
    
    /**
     * 暂停测试任务
     */
    @PutMapping("/test-task/{taskId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseTestTask(@PathVariable String taskId) {
        testTaskScheduler.pauseTest(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 恢复测试任务
     */
    @PutMapping("/test-task/{taskId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeTestTask(@PathVariable String taskId) {
        testTaskScheduler.resumeTest(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 停止测试任务
     */
    @PutMapping("/test-task/{taskId}/stop")
    public ResponseEntity<ApiResponse<Void>> stopTestTask(
            @PathVariable String taskId,
            @RequestBody StopTestRequest request) {
        
        testTaskScheduler.stopTest(taskId, request.getEmergencyToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 获取实时监控数据
     */
    @GetMapping("/monitor/metrics/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealTimeMetrics(@PathVariable String taskId) {
        Map<String, Object> metrics = fixPressureService.getRealTimeMetrics(taskId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    /**
     * 环境检查
     */
    @GetMapping("/system/env-check")
    public ResponseEntity<ApiResponse<EnvironmentCheckResult>> checkEnvironment(
            @RequestParam(required = false) String targetEngineIp) {
        
        EnvironmentCheckResult result = performEnvironmentCheck(targetEngineIp);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 获取测试任务列表
     */
    @GetMapping("/test-task")
    public ResponseEntity<ApiResponse<List<TestTask>>> getTestTasks() {
        List<TestTask> tasks = testTaskScheduler.getTaskList();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
    
    /**
     * 获取测试任务详情
     */
    @GetMapping("/test-task/{taskId}")
    public ResponseEntity<ApiResponse<TestTask>> getTestTask(@PathVariable String taskId) {
        TestTask task = testTaskScheduler.getTaskDetail(taskId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }
    
    // 辅助方法
    private EnvironmentCheckResult performEnvironmentCheck(String targetIp) {
        EnvironmentCheckResult result = new EnvironmentCheckResult();
        
        // 检查目标FIX引擎
        result.setEngineStatus(checkFixEngineStatus(targetIp));
        
        // 检查系统资源
        Runtime runtime = Runtime.getRuntime();
        result.setCpuUsage(getCpuUsage());
        result.setMemoryUsage(getMemoryUsage());
        
        // 检查网络连通性
        result.setNetworkStatus(checkNetworkStatus(targetIp));
        
        return result;
    }
}