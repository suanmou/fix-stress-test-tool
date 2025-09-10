package com.fix.stresstest.dto;

import java.util.Date;
import java.util.Map;

public class TestResultDTO {
    private String testId;
    private TestConfigDTO config;
    private Date startTime;
    private Date endTime;
    private long durationMs;
    
    // 总体统计
    private int totalMessages;
    private int successfulMessages;
    private int failedMessages;
    private double successRate;
    private double throughput; // 消息/秒
    
    // 响应时间统计(毫秒)
    private long minResponseTime;
    private long maxResponseTime;
    private double avgResponseTime;
    private Map<String, Long> percentileResponseTimes; // P90, P95, P99等
    
    // 错误分布
    private Map<String, Integer> errorDistribution;
    
    // GCP指标
    private GcpMetricsDTO gcpMetrics;

    // getter和setter方法
    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public TestConfigDTO getConfig() {
        return config;
    }

    public void setConfig(TestConfigDTO config) {
        this.config = config;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public int getSuccessfulMessages() {
        return successfulMessages;
    }

    public void setSuccessfulMessages(int successfulMessages) {
        this.successfulMessages = successfulMessages;
    }

    public int getFailedMessages() {
        return failedMessages;
    }

    public void setFailedMessages(int failedMessages) {
        this.failedMessages = failedMessages;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public void setMinResponseTime(long minResponseTime) {
        this.minResponseTime = minResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(long maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public Map<String, Long> getPercentileResponseTimes() {
        return percentileResponseTimes;
    }

    public void setPercentileResponseTimes(Map<String, Long> percentileResponseTimes) {
        this.percentileResponseTimes = percentileResponseTimes;
    }

    public Map<String, Integer> getErrorDistribution() {
        return errorDistribution;
    }

    public void setErrorDistribution(Map<String, Integer> errorDistribution) {
        this.errorDistribution = errorDistribution;
    }

    public GcpMetricsDTO getGcpMetrics() {
        return gcpMetrics;
    }

    public void setGcpMetrics(GcpMetricsDTO gcpMetrics) {
        this.gcpMetrics = gcpMetrics;
    }
}
    