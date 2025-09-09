package com.fix.test.dto;

import javax.validation.constraints.*;
import java.util.List;

public class CreateTestPlanRequest {
    
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称长度不能超过100字符")
    private String planName;
    
    @NotNull(message = "FIX版本不能为空")
    private TestPlan.FixVersion fixVersion;
    
    @NotNull(message = "会话数不能为空")
    @Min(value = 10, message = "会话数不能小于10")
    @Max(value = 1000, message = "会话数不能大于1000")
    private Integer sessionCount;
    
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;
    
    private String testOwner;
    private String tags;
    
    @NotEmpty(message = "TPS阶梯不能为空")
    private List<TpsStepRequest> tpsSteps;
    
    @NotEmpty(message = "消息配置不能为空")
    private List<MessageConfigRequest> messageConfigs;
    
    // Getters and Setters
    
    public static class TpsStepRequest {
        @NotNull(message = "TPS不能为空")
        @Min(1) @Max(5000)
        private Integer tps;
        
        @NotNull(message = "持续时间不能为空")
        @Min(1) @Max(3600)
        private Integer duration;
        
        private String remark;
        
        // Getters and Setters
    }
    
    public static class MessageConfigRequest {
        @NotBlank(message = "消息类型不能为空")
        private String msgType;
        
        @NotNull(message = "消息比例不能为空")
        @Min(0) @Max(100)
        private Integer msgRatio;
        
        private Integer maxMsgSize;
        private Integer largeMsgRatio;
        
        // Getters and Setters
    }
}