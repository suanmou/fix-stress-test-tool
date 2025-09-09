package com.fix.test.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_plan")
public class TestPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称长度不能超过100字符")
    private String planName;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private FixVersion fixVersion;
    
    @Column(nullable = false)
    @Min(value = 10, message = "会话数不能小于10")
    @Max(value = 1000, message = "会话数不能大于1000")
    private Integer sessionCount;
    
    @Column(length = 500)
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;
    
    @Column(length = 50)
    private String testOwner;
    
    @Column(length = 200)
    private String tags;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status = PlanStatus.DRAFT;
    
    @Column(nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
    
    @OneToMany(mappedBy = "testPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TpsStep> tpsSteps;
    
    @OneToMany(mappedBy = "testPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageConfig> messageConfigs;
    
    // 枚举定义
    public enum FixVersion {
        FIX_4_2, FIX_4_4, FIX_5_0_SP2
    }
    
    public enum PlanStatus {
        DRAFT, CONFIGURED, READY, RUNNING, COMPLETED, FAILED
    }
    
    // Getters and Setters
}