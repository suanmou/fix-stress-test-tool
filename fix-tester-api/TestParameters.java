package com.finance.fix.tester.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 压力测试参数实体类
 * 接收前端传递的测试配置参数
 */
public class TestParameters {
    // 并发会话数量
    @JsonProperty("sessions")
    private int sessions;
    
    // 每秒消息数
    @JsonProperty("rate")
    private int rate;
    
    // 总消息数量（与duration二选一）
    @JsonProperty("messages")
    private Integer messages;
    
    // 测试持续时间（分钟，与messages二选一）
    @JsonProperty("duration")
    private Integer duration;
    
    // 响应超时时间（秒）
    @JsonProperty("timeout")
    private int timeout = 5;
    
    // 配置文件路径
    @JsonProperty("config_path")
    private String configPath = "fixconfig.template";
    
    // 是否启用详细日志
    @JsonProperty("detailed_log")
    private boolean detailedLog = false;

    // getter和setter方法
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
}
    