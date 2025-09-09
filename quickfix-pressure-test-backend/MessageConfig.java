package com.fix.test.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;

@Entity
@Table(name = "test_plan_message_config")
public class MessageConfig implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TestPlan testPlan;
    
    @Column(nullable = false, length = 10)
    private String msgType; // 35=D, 35=F, etc.
    
    @Column(nullable = false)
    @Min(value = 0)
    @Max(value = 100)
    private Integer msgRatio; // 百分比
    
    @Column
    private Integer maxMsgSize; // KB
    
    @Column
    private Integer largeMsgRatio; // 百分比
    
    // Getters and Setters
}