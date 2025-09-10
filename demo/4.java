package com.fixstress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportGenerator {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public static void generateReport(TestConfig config, TestStatistics stats, String outputPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode report = mapper.createObjectNode();
            
            // 测试配置
            ObjectNode configNode = mapper.valueToTree(config);
            report.set("config", configNode);
            
            // 测试统计
            ObjectNode statsNode = mapper.createObjectNode();
            statsNode.put("messagesSent", stats.getMessagesSent());
            statsNode.put("messagesReceived", stats.getMessagesReceived());
            statsNode.put("errorCount", stats.getErrorCount());
            statsNode.put("elapsedTime", stats.getElapsedTime());
            statsNode.put("messagesPerSecond", stats.getMessagesPerSecond());
            statsNode.put("averageLatency", stats.getAverageLatency());
            statsNode.put("minLatency", stats.getMinLatency());
            statsNode.put("maxLatency", stats.getMaxLatency());
            statsNode.put("errorRate", stats.getErrorRate());
            
            report.set("statistics", statsNode);
            
            // 测试结果摘要
            ObjectNode summaryNode = mapper.createObjectNode();
            summaryNode.put("throughputAchieved", stats.getMessagesPerSecond() >= config.getMsgRate() * 0.9);
            summaryNode.put("latencyAcceptable", stats.getAverageLatency() < 100); // 100ms阈值
            summaryNode.put("errorRateAcceptable", stats.getErrorRate() < 1.0); // 1%错误率阈值
            
            report.set("summary", summaryNode);
            
            // 生成建议
            String recommendations = generateRecommendations(config, stats);
            report.put("recommendations", recommendations);
            
            // 保存报告
            String timestamp = DATE_FORMAT.format(new Date());
            String filename = String.format("stress_test_report_%s.json", timestamp);
            File outputFile = new File(outputPath, filename);
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, report);
            
            System.out.println("Report generated: " + outputFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
    
    private static String generateRecommendations(TestConfig config, TestStatistics stats) {
        StringBuilder sb = new StringBuilder();
        
        // 吞吐量建议
        if (stats.getMessagesPerSecond() < config.getMsgRate() * 0.9) {
            sb.append("系统未能达到目标吞吐量。建议：\n");
            sb.append("- 检查网络带宽和延迟\n");
            sb.append("- 检查FIX引擎的处理能力\n");
            sb.append("- 考虑增加客户端实例数量进行分布式测试\n\n");
        } else {
            sb.append("系统达到了目标吞吐量。表现良好。\n\n");
        }
        
        // 延迟建议
        if (stats.getAverageLatency() > 100) {
            sb.append("系统延迟较高。建议：\n");
            sb.append("- 优化FIX引擎的消息处理逻辑\n");
            sb.append("- 检查网络延迟和带宽\n");
            sb.append("- 考虑使用更高效的硬件或优化JVM参数\n\n");
        } else {
            sb.append("系统延迟在可接受范围内。\n\n");
        }
        
        // 错误率建议
        if (stats.getErrorRate() > 1.0) {
            sb.append("系统错误率较高。建议：\n");
            sb.append("- 检查FIX会话配置\n");
            sb.append("- 验证消息格式和字段有效性\n");
            sb.append("- 检查网络稳定性\n\n");
        } else {
            sb.append("系统错误率在可接受范围内。\n\n");
        }
        
        // 通用建议
        sb.append("通用建议：\n");
        sb.append("- 在生产环境中进行更长时间的耐力测试\n");
        sb.append("- 监控系统资源使用情况（CPU、内存、网络）\n");
        sb.append("- 设置适当的警报阈值\n");
        
        return sb.toString();
    }
}