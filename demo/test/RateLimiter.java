package com.finance.fix.tester;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简单的令牌桶实现，控制每秒发送的消息数量
 */
public class RateLimiter {
    private final long ratePerSecond;
    private final long maxBurstTokens;
    
    private double availableTokens;
    private long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();
    
    private RateLimiter(long ratePerSecond, long maxBurstTokens) {
        this.ratePerSecond = ratePerSecond;
        this.maxBurstTokens = maxBurstTokens;
        this.availableTokens = maxBurstTokens;
        this.lastRefillTime = System.nanoTime();
    }
    
    public static RateLimiter create(long ratePerSecond) {
        return new RateLimiter(ratePerSecond, ratePerSecond);
    }
    
    public void acquire() throws InterruptedException {
        acquire(1);
    }
    
    public void acquire(int tokens) throws InterruptedException {
        long waitTime = reserve(tokens);
        if (waitTime > 0) {
            TimeUnit.NANOSECONDS.sleep(waitTime);
        }
    }
    
    private long reserve(int tokens) {
        lock.lock();
        try {
            refill();
            
            double needTokens = tokens;
            if (availableTokens >= needTokens) {
                availableTokens -= needTokens;
                return 0;
            } else {
                // 需要等待的令牌数量
                double need = needTokens - availableTokens;
                // 计算需要等待的时间（纳秒）
                long waitTime = (long) (need * 1_000_000_000 / ratePerSecond);
                availableTokens = 0;
                return waitTime;
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTime;
        
        if (elapsedNanos > 0) {
            // 计算这段时间内可以生成的令牌数
            double newTokens = (elapsedNanos / 1_000_000_000.0) * ratePerSecond;
            availableTokens = Math.min(availableTokens + newTokens, maxBurstTokens);
            lastRefillTime = now;
        }
    }
}
    