package com.fix.test.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_task")
public class TestTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String taskId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TestPlan testPlan;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.STARTING;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDuration; // 毫秒
    
    @Column(length = 50)
    private String emergencyToken;
    
    @Column
    private String errorMessage;
    
    @Column
    private Integer currentTps;
    
    @Column
    private Integer currentStep;
    
    @Column
    private Integer totalSteps;
    
    @OneToMany(mappedBy = "testTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TaskStepProgress> stepProgress;
    
    public enum TaskStatus {
        STARTING, RUNNING, PAUSED, COMPLETED, FAILED, STOPPING
    }
    
    // Getters and Setters
}