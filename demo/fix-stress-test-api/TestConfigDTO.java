package com.fix.stresstest.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TestConfigDTO {
    // FIX服务器主机地址
    @NotBlank(message = "服务器地址不能为空")
    private String host;
    
    // FIX服务器端口
    @NotNull(message = "端口不能为空")
    private Integer port;
    
    // FIX版本
    private String fixVersion = "FIX.4.4";
    
    // 发送方CompID
    @NotBlank(message = "发送方ID不能为空")
    private String senderCompId;
    
    // 目标方CompID
    @NotBlank(message = "目标方ID不能为空")
    private String targetCompId;
    
    // 并发客户端数量
    @Min(value = 1, message = "并发数至少为1")
    private int concurrentClients = 1;
    
    // 每个客户端发送的消息数量
    @Min(value = 1, message = "消息数至少为1")
    private int messagesPerClient = 100;
    
    // 消息发送间隔(毫秒)
    private int sendInterval = 0;
    
    // 是否收集GCP指标
    private boolean collectGcpMetrics = false;
    
    // GCP项目ID
    private String gcpProjectId;
    
    // GCP实例ID
    private String gcpInstanceId;
    
    // GCP区域
    private String gcpZone;

    // getter和setter方法
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getFixVersion() {
        return fixVersion;
    }

    public void setFixVersion(String fixVersion) {
        this.fixVersion = fixVersion;
    }

    public String getSenderCompId() {
        return senderCompId;
    }

    public void setSenderCompId(String senderCompId) {
        this.senderCompId = senderCompId;
    }

    public String getTargetCompId() {
        return targetCompId;
    }

    public void setTargetCompId(String targetCompId) {
        this.targetCompId = targetCompId;
    }

    public int getConcurrentClients() {
        return concurrentClients;
    }

    public void setConcurrentClients(int concurrentClients) {
        this.concurrentClients = concurrentClients;
    }

    public int getMessagesPerClient() {
        return messagesPerClient;
    }

    public void setMessagesPerClient(int messagesPerClient) {
        this.messagesPerClient = messagesPerClient;
    }

    public int getSendInterval() {
        return sendInterval;
    }

    public void setSendInterval(int sendInterval) {
        this.sendInterval = sendInterval;
    }

    public boolean isCollectGcpMetrics() {
        return collectGcpMetrics;
    }

    public void setCollectGcpMetrics(boolean collectGcpMetrics) {
        this.collectGcpMetrics = collectGcpMetrics;
    }

    public String getGcpProjectId() {
        return gcpProjectId;
    }

    public void setGcpProjectId(String gcpProjectId) {
        this.gcpProjectId = gcpProjectId;
    }

    public String getGcpInstanceId() {
        return gcpInstanceId;
    }

    public void setGcpInstanceId(String gcpInstanceId) {
        this.gcpInstanceId = gcpInstanceId;
    }

    public String getGcpZone() {
        return gcpZone;
    }

    public void setGcpZone(String gcpZone) {
        this.gcpZone = gcpZone;
    }
}
    