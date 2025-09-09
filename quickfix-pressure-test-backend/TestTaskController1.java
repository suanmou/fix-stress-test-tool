package com.fix.test.controller;

import com.fix.test.dto.*;
import com.fix.test.entity.TestTask;
import com.fix.test.service.TestTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/test-task")
public class TestTaskController {
    
    @Autowired
    private TestTaskService testTaskService;
    
    /**
     * 创建测试任务
     */
    @PostMapping("/create/{planId}")
    public ResponseEntity<ApiResponse<TestTask>> createTask(
            @PathVariable Long planId,
            @Valid @RequestBody CreateTestTaskRequest request) {
        
        TestTask task = testTaskService.createTestTask(planId, request);
        return ResponseEntity.ok(ApiResponse.success(task));
    }
    
    /**
     * 启动测试任务
     */
    @PostMapping("/{taskId}/start")
    public ResponseEntity<ApiResponse<Void>> startTask(@PathVariable String taskId) {
        testTaskService.startTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 暂停测试任务
     */
    @PutMapping("/{taskId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseTask(@PathVariable String taskId) {
        testTaskService.pauseTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 恢复测试任务
     */
    @PutMapping("/{taskId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeTask(@PathVariable String taskId) {
        testTaskService.resumeTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 停止测试任务
     */
    @PutMapping("/{taskId}/stop")
    public ResponseEntity<ApiResponse<Void>> stopTask(
            @PathVariable String taskId,
            @RequestParam String emergencyToken) {
        
        testTaskService.stopTask(taskId, emergencyToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 获取测试任务列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestTaskSummary>>> getTaskList() {
        List<TestTaskSummary> tasks = testTaskService.getTaskList();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
    
    /**
     * 获取测试任务详情
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TestTaskDetail>> getTaskDetail(@PathVariable String taskId) {
        TestTaskDetail detail = testTaskService.getTaskDetail(taskId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
    
    /**
     * 获取任务实时指标
     */
    @GetMapping("/{taskId}/metrics")
    public ResponseEntity<ApiResponse<TaskMetrics>> getTaskMetrics(@PathVariable String taskId) {
        TaskMetrics metrics = testTaskService.getCurrentMetrics(taskId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}