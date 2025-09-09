package com.fix.test.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MonitorDataCollector {
    
    @Autowired
    private InfluxDBClient influxDBClient;
    
    @Autowired
    private FixPressureService fixPressureService;
    
    private final Map<String, AtomicBoolean> collectingFlags = new ConcurrentHashMap<>();
    private final Map<String, Thread> collectorThreads = new ConcurrentHashMap<>();
    
    /**
     * 开始采集监控数据
     */
    public void startCollection(String taskId) {
        collectingFlags.put(taskId, new AtomicBoolean(true));
        
        Thread collectorThread = new Thread(() -> {
            while (collectingFlags.get(taskId).get()) {
                try {
                    collectMetrics(taskId);
                    Thread.sleep(1000); // 每秒采集一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        collectorThread.start();
        collectorThreads.put(taskId, collectorThread);
    }
    
    /**
     * 采集监控指标
     */
    private void collectMetrics(String taskId) {
        Map<String, Object> metrics = fixPressureService.getRealTimeMetrics(taskId);
        
        Point point = Point.measurement("test_metrics")
            .addTag("task_id", taskId)
            .addField("tps", metrics.getOrDefault("currentTps", 0))
            .addField("total_sent", metrics.getOrDefault("totalSent", 0L))
            .addField("total_received", metrics.getOrDefault("totalReceived", 0L))
            .addField("total_failed", metrics.getOrDefault("totalFailed", 0L))
            .addField("session_count", metrics.getOrDefault("sessionCount", 0))
            .addField("is_running", metrics.getOrDefault("isRunning", false))
            .time(Instant.now(), WritePrecision.NS);
        
        // 采集系统资源
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        point.addField("memory_used", usedMemory / 1024 / 1024); // MB
        point.addField("memory_total", totalMemory / 1024 / 1024); // MB
        
        // 写入InfluxDB
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(point);
        }
    }
    
    /**
     * 停止采集
     */
    public void stopCollection(String taskId) {
        AtomicBoolean flag = collectingFlags.get(taskId);
        if (flag != null) {
            flag.set(false);
        }
        
        Thread thread = collectorThreads.get(taskId);
        if (thread != null) {
            thread.interrupt();
            collectorThreads.remove(taskId);
        }
        
        collectingFlags.remove(taskId);
    }
}