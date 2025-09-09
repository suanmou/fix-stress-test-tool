package com.fix.test.dto;

import lombok.Data;

@Data
public class CreateTestTaskRequest {
    private String emergencyToken;
}

@Data
public class TestTaskSummary {
    private String taskId;
    private String planName;
    private TestTask.TaskStatus status;
    private Integer currentTps;
    private Double progress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDuration;
}

@Data
public class TestTaskDetail {
    private TestTask task;
    private List<TaskStepProgress> steps;
    private TaskMetrics metrics;
}

@Data
public class TaskMetrics {
    private Long totalMessagesSent;
    private Long totalMessagesReceived;
    private Long totalMessagesFailed;
    private Integer currentStep;
    private Integer currentTps;
    private Double progress;
    private Integer sessionCount;
}