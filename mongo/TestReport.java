package com.finance.fix.tester.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 测试报告实体类，存储在MongoDB中
 */
@Document(collection = "test_reports")
public class TestReport {

    @Id
    private String id;                          // 报告ID，与测试任务ID一致
    private String configId;                    // 关联的测试配置ID
    private String configName;                  // 关联的测试配置名称
    private LocalDateTime startTime;            // 测试开始时间
    private LocalDateTime endTime;              // 测试结束时间
    private String status;                      // 测试状态：RUNNING, COMPLETED, FAILED, STOPPED
    
    // 测试参数(冗余存储，避免配置变更影响历史报告)
    private int totalSessions;                  // 会话总数
    private int targetRate;                     // 目标TPS
    private Integer totalMessages;              // 计划消息总数
    private Integer durationMinutes;            // 计划持续时间(分钟)
    private int timeoutSeconds;                 // 超时设置(秒)
    
    // 测试结果统计
    private long totalMessagesSent;             // 实际发送消息数
    private long totalResponsesReceived;        // 实际接收响应数
    private long timeoutCount;                  // 超时消息数
    private double actualRate;                  // 实际平均TPS
    private double averageResponseTime;         // 平均响应时间(ms)
    private double p95ResponseTime;             // 95%响应时间(ms)
    private double p99ResponseTime;             // 99%响应时间(ms)
    
    // 连接统计
    private double connectionSuccessRate;       // 连接成功率(%)
    private double averageConnectionTime;       // 平均连接时间(ms)
    private List<ConnectionError> connectionErrors; // 连接错误详情
    
    // 系统指标
    private Map<String, List<SystemMetric>> systemMetrics; // 系统指标时间序列数据
    
    // 错误信息(如果测试失败)
    private String errorMessage;
    
    // 扩展字段
    private Map<String, Object> extraFields;

    // 内部类：连接错误详情
    public static class ConnectionError {
        private String type;    // 错误类型
        private int count;      // 错误次数
        private String message; // 错误消息
        
        // 构造函数、getter和setter
        public ConnectionError(String type, int count, String message) {
            this.type = type;
            this.count = count;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    // 内部类：系统指标
    public static class SystemMetric {
        private LocalDateTime timestamp;  // 时间戳
        private double value;             // 指标值
        private String unit;              // 单位
        
        // 构造函数、getter和setter
        public SystemMetric(LocalDateTime timestamp, double value, String unit) {
            this.timestamp = timestamp;
            this.value = value;
            this.unit = unit;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getTargetRate() {
        return targetRate;
    }

    public void setTargetRate(int targetRate) {
        this.targetRate = targetRate;
    }

    public Integer getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public long getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public void setTotalMessagesSent(long totalMessagesSent) {
        this.totalMessagesSent = totalMessagesSent;
    }

    public long getTotalResponsesReceived() {
        return totalResponsesReceived;
    }

    public void setTotalResponsesReceived(long totalResponsesReceived) {
        this.totalResponsesReceived = totalResponsesReceived;
    }

    public long getTimeoutCount() {
        return timeoutCount;
    }

    public void setTimeoutCount(long timeoutCount) {
        this.timeoutCount = timeoutCount;
    }

    public double getActualRate() {
        return actualRate;
    }

    public void setActualRate(double actualRate) {
        this.actualRate = actualRate;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double getP95ResponseTime() {
        return p95ResponseTime;
    }

    public void setP95ResponseTime(double p95ResponseTime) {
        this.p95ResponseTime = p95ResponseTime;
    }

    public double getP99ResponseTime() {
        return p99ResponseTime;
    }

    public void setP99ResponseTime(double p99ResponseTime) {
        this.p99ResponseTime = p99ResponseTime;
    }

    public double getConnectionSuccessRate() {
        return connectionSuccessRate;
    }

    public void setConnectionSuccessRate(double connectionSuccessRate) {
        this.connectionSuccessRate = connectionSuccessRate;
    }

    public double getAverageConnectionTime() {
        return averageConnectionTime;
    }

    public void setAverageConnectionTime(double averageConnectionTime) {
        this.averageConnectionTime = averageConnectionTime;
    }

    public List<ConnectionError> getConnectionErrors() {
        return connectionErrors;
    }

    public void setConnectionErrors(List<ConnectionError> connectionErrors) {
        this.connectionErrors = connectionErrors;
    }

    public Map<String, List<SystemMetric>> getSystemMetrics() {
        return systemMetrics;
    }

    public void setSystemMetrics(Map<String, List<SystemMetric>> systemMetrics) {
        this.systemMetrics = systemMetrics;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields;
    }
}
    