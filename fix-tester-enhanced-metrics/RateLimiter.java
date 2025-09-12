package com.finance.fix.tester;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于令牌桶算法的速率限制器
 */
public class RateLimiter {
    private final long ratePerSecond;
    private final long maxBurstTokens;
    
    private double availableTokens;
    private long lastRefillTimestamp;
    private final ReentrantLock lock = new ReentrantLock();

    public RateLimiter(long ratePerSecond) {
        this(ratePerSecond, ratePerSecond);
    }

    public RateLimiter(long ratePerSecond, long maxBurstTokens) {
        this.ratePerSecond = ratePerSecond;
        this.maxBurstTokens = maxBurstTokens;
        this.availableTokens = maxBurstTokens;
        this.lastRefillTimestamp = System.nanoTime();
    }

    /**
     * 获取一个令牌，如果没有可用令牌则等待
     */
    public void acquire() throws InterruptedException {
        acquire(1);
    }

    /**
     * 获取指定数量的令牌，如果没有足够令牌则等待
     */
    public void acquire(int tokens) throws InterruptedException {
        long waitTime = reserve(tokens);
        if (waitTime > 0) {
            TimeUnit.NANOSECONDS.sleep(waitTime);
        }
    }

    /**
     * 预留令牌并返回需要等待的纳秒数
     */
    private long reserve(int tokens) {
        lock.lock();
        try {
            refill();
            
            double neededTokens = tokens;
            long waitTime = 0;
            
            // 如果没有足够的令牌，计算需要等待的时间
            if (availableTokens < neededTokens) {
                // 需要的令牌数超过当前可用，计算还需要多少
                double deficit = neededTokens - availableTokens;
                waitTime = (long) (deficit * 1_000_000_000 / ratePerSecond);
            }
            
            // 消耗令牌
            availableTokens -= neededTokens;
            if (availableTokens < 0) {
                availableTokens = 0;
            }
            
            return waitTime;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据时间流逝补充令牌
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTimestamp;
        
        if (elapsedNanos > 0) {
            // 计算这段时间应该补充的令牌数
            double newTokens = (elapsedNanos / 1_000_000_000.0) * ratePerSecond;
            availableTokens = Math.min(availableTokens + newTokens, maxBurstTokens);
            lastRefillTimestamp = now;
        }
    }
}
    