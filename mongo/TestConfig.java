package com.finance.fix.tester.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试配置实体类，存储在MongoDB中
 */
@Document(collection = "test_configs")
public class TestConfig {

    @Id
    private String id;                     // MongoDB自动生成的ID
    private String name;                   // 配置名称，用于识别
    private String description;            // 配置描述
    private String createdBy;              // 创建者
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
    private boolean isDefault;             // 是否为默认配置
    
    // 测试核心参数
    private int sessions;                  // 会话数量
    private int rate;                      // 消息速率(TPS)
    private Integer messages;              // 消息总数(可选)
    private Integer duration;              // 持续时间(分钟，可选)
    private int timeout;                   // 超时时间(秒)
    private String configPath;             // FIX配置文件路径
    private boolean detailedLog;           // 是否启用详细日志
    private boolean recordSystemMetrics;   // 是否记录系统指标
    
    // 扩展参数，用于存储未来可能的新参数
    private Map<String, Object> extraParams = new HashMap<>();

    // 构造函数、getter和setter
    public TestConfig() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public int getSessions() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public Integer getMessages() {
        return messages;
    }

    public void setMessages(Integer messages) {
        this.messages = messages;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public boolean isDetailedLog() {
        return detailedLog;
    }

    public void setDetailedLog(boolean detailedLog) {
        this.detailedLog = detailedLog;
    }

    public boolean isRecordSystemMetrics() {
        return recordSystemMetrics;
    }

    public void setRecordSystemMetrics(boolean recordSystemMetrics) {
        this.recordSystemMetrics = recordSystemMetrics;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
    }
    
    // 更新时间戳方法
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
    