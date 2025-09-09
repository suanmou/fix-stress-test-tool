package com.fix.test.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;

@Entity
@Table(name = "test_plan_tps_step")
public class TpsStep implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TestPlan testPlan;
    
    @Column(nullable = false)
    @Min(value = 1, message = "TPS必须大于0")
    @Max(value = 5000, message = "TPS不能超过5000")
    private Integer tps;
    
    @Column(nullable = false)
    @Min(value = 1, message = "持续时间必须大于0")
    @Max(value = 3600, message = "持续时间不能超过3600分钟")
    private Integer duration; // 分钟
    
    @Column(length = 200)
    private String remark;
    
    @Column(nullable = false)
    private Integer stepOrder;
    
    // Getters and Setters
}