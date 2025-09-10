package com.fixstress;

import java.util.concurrent.atomic.AtomicLong;

public class TestStatistics {
    private AtomicLong messagesSent = new AtomicLong(0);
    private AtomicLong messagesReceived = new AtomicLong(0);
    private AtomicLong errorCount = new AtomicLong(0);
    private long startTime;
    private long endTime;
    
    // 延迟统计
    private long minLatency = Long.MAX_VALUE;
    private long maxLatency = Long.MIN_VALUE;
    private long totalLatency = 0;
    private long latencyCount = 0;
    
    public TestStatistics() {}
    
    public void incrementMessagesSent() {
        messagesSent.incrementAndGet();
    }
    
    public void incrementMessagesReceived() {
        messagesReceived.incrementAndGet();
    }
    
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }
    
    public void recordLatency(long latency) {
        if (latency < minLatency) minLatency = latency;
        if (latency > maxLatency) maxLatency = latency;
        totalLatency += latency;
        latencyCount++;
    }
    
    public void start() {
        startTime = System.currentTimeMillis();
    }
    
    public void stop() {
        endTime = System.currentTimeMillis();
    }
    
    public long getElapsedTime() {
        if (startTime == 0) return 0;
        long currentEnd = endTime > 0 ? endTime : System.currentTimeMillis();
        return (currentEnd - startTime) / 1000;
    }
    
    public double getMessagesPerSecond() {
        long elapsedSeconds = getElapsedTime();
        if (elapsedSeconds == 0) return 0;
        return (double) messagesSent.get() / elapsedSeconds;
    }
    
    public double getAverageLatency() {
        if (latencyCount == 0) return 0;
        return (double) totalLatency / latencyCount;
    }
    
    public long getMinLatency() {
        return minLatency == Long.MAX_VALUE ? 0 : minLatency;
    }
    
    public long getMaxLatency() {
        return maxLatency == Long.MIN_VALUE ? 0 : maxLatency;
    }
    
    public double getErrorRate() {
        long totalMessages = messagesSent.get();
        if (totalMessages == 0) return 0;
        return (double) errorCount.get() / totalMessages * 100;
    }
    
    // Getter方法
    public long getMessagesSent() { return messagesSent.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
}