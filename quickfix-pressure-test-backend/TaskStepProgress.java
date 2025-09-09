package com.fix.test.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_step_progress")
public class TaskStepProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TestTask testTask;
    
    @Column(nullable = false)
    private Integer stepNumber;
    
    @Column(nullable = false)
    private Integer targetTps;
    
    @Column
    private Integer actualTps;
    
    @Column
    private LocalDateTime startTime;
    
    @Column
    private LocalDateTime endTime;
    
    @Column
    private Long duration; // 毫秒
    
    @Column
    private Long messagesSent;
    
    @Column
    private Long messagesReceived;
    
    @Column
    private Long messagesFailed;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StepStatus status = StepStatus.PENDING;
    
    public enum StepStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    }
    
    // Getters and Setters
}