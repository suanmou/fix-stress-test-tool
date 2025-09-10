package com.financial.fix.stresstest;

import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GCP指标收集器，用于从GCP监控服务获取相关资源指标
 */
public class GcpMetricsCollector {
    private final String projectId;
    private final MetricServiceClient metricClient;
    private final String vmInstanceId; // 被测试的FIX引擎所在VM实例ID
    private final String zone; // VM所在区域

    // 要收集的GCP指标名称
    private static final String CPU_USAGE_METRIC = "compute.googleapis.com/instance/cpu/utilization";
    private static final String NETWORK_RX_METRIC = "compute.googleapis.com/instance/network/received_bytes_count";
    private static final String NETWORK_TX_METRIC = "compute.googleapis.com/instance/network/sent_bytes_count";
    private static final String NETWORK_LATENCY_METRIC = "compute.googleapis.com/instance/network/effective_rtt";
    private static final String FIREWALL_DROPPED_METRIC = "compute.googleapis.com/firewall/dropped_bytes_count";

    public GcpMetricsCollector(String projectId, String vmInstanceId, String zone) throws Exception {
        this.projectId = projectId;
        this.vmInstanceId = vmInstanceId;
        this.zone = zone;
        this.metricClient = MetricServiceClient.create();
    }

    /**
     * 收集指定时间段内的GCP指标
     */
    public Map<String, Double> collectMetrics(Instant startTime, Instant endTime) {
        Map<String, Double> metrics = new HashMap<>();
        
        // 构建时间区间
        TimeInterval interval = TimeInterval.newBuilder()
                .setStartTime(Timestamps.fromMillis(startTime.toEpochMilli()))
                .setEndTime(Timestamps.fromMillis(endTime.toEpochMilli()))
                .build();

        // 收集CPU使用率
        double cpuUsage = getMetricAverage(CPU_USAGE_METRIC, interval);
        metrics.put("cpu_usage_average", cpuUsage);
        
        // 收集网络接收速率
        double networkRx = getMetricRate(NETWORK_RX_METRIC, interval);
        metrics.put("network_receive_bytes_per_sec", networkRx);
        
        // 收集网络发送速率
        double networkTx = getMetricRate(NETWORK_TX_METRIC, interval);
        metrics.put("network_transmit_bytes_per_sec", networkTx);
        
        // 收集网络延迟
        double networkLatency = getMetricAverage(NETWORK_LATENCY_METRIC, interval);
        metrics.put("network_latency_ms", networkLatency);
        
        // 收集防火墙丢弃字节数
        double firewallDropped = getMetricRate(FIREWALL_DROPPED_METRIC, interval);
        metrics.put("firewall_dropped_bytes_per_sec", firewallDropped);
        
        return metrics;
    }

    /**
     * 获取指标的平均值
     */
    private double getMetricAverage(String metricType, TimeInterval interval) {
        ListTimeSeriesRequest request = buildTimeSeriesRequest(metricType, interval);
        UnaryCallable<ListTimeSeriesRequest, List<TimeSeries>> callable = metricClient.listTimeSeriesCallable();
        
        try {
            List<TimeSeries> timeSeriesList = callable.call(request);
            if (timeSeriesList.isEmpty()) {
                return 0.0;
            }
            
            // 计算平均值
            double sum = 0.0;
            int count = 0;
            
            for (TimeSeries ts : timeSeriesList) {
                for (var point : ts.getPointsList()) {
                    TypedValue value = point.getValue();
                    if (value.hasDoubleValue()) {
                        sum += value.getDoubleValue();
                        count++;
                    }
                }
            }
            
            return count > 0 ? sum / count : 0.0;
        } catch (Exception e) {
            System.err.println("Error collecting metric " + metricType + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * 获取指标的速率（每秒变化量）
     */
    private double getMetricRate(String metricType, TimeInterval interval) {
        ListTimeSeriesRequest request = buildTimeSeriesRequest(metricType, interval);
        
        try {
            List<TimeSeries> timeSeriesList = metricClient.listTimeSeries(request);
            if (timeSeriesList.isEmpty()) {
                return 0.0;
            }
            
            // 计算速率 (总变化量 / 时间秒数)
            long startTime = Timestamps.toMillis(interval.getStartTime());
            long endTime = Timestamps.toMillis(interval.getEndTime());
            double durationSec = (endTime - startTime) / 1000.0;
            
            if (durationSec <= 0) {
                return 0.0;
            }
            
            double totalValue = 0.0;
            for (TimeSeries ts : timeSeriesList) {
                for (var point : ts.getPointsList()) {
                    TypedValue value = point.getValue();
                    if (value.hasInt64Value()) {
                        totalValue += value.getInt64Value();
                    } else if (value.hasDoubleValue()) {
                        totalValue += value.getDoubleValue();
                    }
                }
            }
            
            return totalValue / durationSec;
        } catch (Exception e) {
            System.err.println("Error collecting metric rate " + metricType + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * 构建时间序列请求
     */
    private ListTimeSeriesRequest buildTimeSeriesRequest(String metricType, TimeInterval interval) {
        String filter = String.format(
            "metric.type=\"%s\" AND resource.labels.instance_id=\"%s\" AND resource.labels.zone=\"%s\"",
            metricType, vmInstanceId, zone
        );
        
        return ListTimeSeriesRequest.newBuilder()
                .setName(ProjectName.of(projectId).toString())
                .setFilter(filter)
                .setInterval(interval)
                .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
                .build();
    }

    /**
     * 关闭客户端释放资源
     */
    public void close() {
        if (metricClient != null) {
            metricClient.close();
        }
    }
}
    