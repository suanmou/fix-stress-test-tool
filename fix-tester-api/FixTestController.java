package com.finance.fix.tester.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fix-test")
public class FixTestController {

    @Autowired
    private FixTestService testService;

    /**
     * 启动新的压力测试任务
     * @param testParameters 测试参数
     * @return 包含任务ID的响应
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startTest(@RequestBody TestParameters testParameters) {
        try {
            // 验证参数
            validateParameters(testParameters);
            
            // 生成唯一任务ID
            String taskId = UUID.randomUUID().toString();
            
            // 启动测试（异步执行）
            testService.startTest(taskId, testParameters);
            
            // 返回任务ID给前端
            return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "status", "TEST_INITIATED",
                "message", "压力测试已启动，请通过taskId查询状态"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "启动测试失败: " + e.getMessage()));
        }
    }

    /**
     * 查询测试任务状态
     * @param taskId 任务ID
     * @return 任务状态和当前结果
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<TestStatusResponse> getTestStatus(@PathVariable String taskId) {
        TestStatusResponse status = testService.getTestStatus(taskId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * 获取测试完整报告
     * @param taskId 任务ID
     * @return 详细测试报告
     */
    @GetMapping("/report/{taskId}")
    public ResponseEntity<TestReport> getTestReport(@PathVariable String taskId) {
        TestReport report = testService.getTestReport(taskId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    /**
     * 终止正在运行的测试
     * @param taskId 任务ID
     * @return 终止结果
     */
    @PostMapping("/stop/{taskId}")
    public ResponseEntity<Map<String, String>> stopTest(@PathVariable String taskId) {
        boolean result = testService.stopTest(taskId);
        if (result) {
            return ResponseEntity.ok(Map.of("status", "TEST_STOPPED", "message", "测试已终止"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 参数验证逻辑
    private void validateParameters(TestParameters params) {
        if (params.getSessions() <= 0 || params.getSessions() > 200) {
            throw new IllegalArgumentException("会话数必须在1-200之间");
        }
        if (params.getRate() <= 0 || params.getRate() > 1000) {
            throw new IllegalArgumentException("消息速率必须在1-1000 TPS之间");
        }
        if (params.getMessages() != null && params.getMessages() <= 0) {
            throw new IllegalArgumentException("消息总数必须大于0");
        }
        if (params.getDuration() != null && params.getDuration() <= 0) {
            throw new IllegalArgumentException("测试时长必须大于0分钟");
        }
        if (params.getMessages() == null && params.getDuration() == null) {
            throw new IllegalArgumentException("必须指定消息总数或测试时长");
        }
    }
}
    