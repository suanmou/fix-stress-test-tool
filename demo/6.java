package com.fixstress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class StressTestApplication {
    private static final Logger logger = LoggerFactory.getLogger(StressTestApplication.class);
    
    public static void main(String[] args) {
        try {
            // 解析命令行参数
            Options options = new Options();
            options.addOption("c", "config", true, "Path to JSON configuration file");
            options.addOption("o", "output", true, "Output directory for reports");
            options.addOption("h", "help", false, "Show help");
            
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            
            if (cmd.hasOption("h")) {
                printHelp(options);
                return;
            }
            
            String configPath = cmd.getOptionValue("c", "config.json");
            String outputPath = cmd.getOptionValue("o", "reports");
            
            // 创建输出目录
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 加载配置
            ObjectMapper mapper = new ObjectMapper();
            TestConfig config = mapper.readValue(new File(configPath), TestConfig.class);
            
            // 创建并初始化客户端
            FixStressTestClient client = new FixStressTestClient(config);
            client.initialize();
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook triggered, stopping test...");
                client.stopTest();
                
                // 生成报告
                TestStatistics stats = client.getStatistics();
                ReportGenerator.generateReport(config, stats, outputPath);
            }));
            
            // 启动测试
            client.startTest();
            
            // 测试完成后生成报告
            TestStatistics stats = client.getStatistics();
            ReportGenerator.generateReport(config, stats, outputPath);
            
        } catch (ParseException e) {
            System.err.println("Error parsing command line options: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error running stress test", e);
        }
    }
    
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("fix-stress-test", options);
    }
}