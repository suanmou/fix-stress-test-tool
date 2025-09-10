package com.financial.fix.stresstest;

/**
 * 存储单个FIX消息的测试结果
 */
public class FixTestResult {
    private final String clOrdId;
    private final long sendTimeMs;
    private boolean success;
    private long responseTimeMs;
    private String errorMessage;

    public FixTestResult(String clOrdId, long sendTimeMs, boolean success, 
                        long responseTimeMs, String errorMessage) {
        this.clOrdId = clOrdId;
        this.sendTimeMs = sendTimeMs;
        this.success = success;
        this.responseTimeMs = responseTimeMs;
        this.errorMessage = errorMessage;
    }

    public String getClOrdId() {
        return clOrdId;
    }

    public long getSendTimeMs() {
        return sendTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
    