package com.fix.stresstest.controller;

import com.fix.stresstest.dto.TestConfigDTO;
import com.fix.stresstest.dto.TestResultDTO;
import com.fix.stresstest.service.FixTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fix-test")
public class FixTestController {

    @Autowired
    private FixTestService fixTestService;

    /**
     * 开始新的压力测试
     */
    @PostMapping("/start")
    public ResponseEntity<String> startTest(@RequestBody TestConfigDTO config) {
        String testId = fixTestService.startTest(config);
        return ResponseEntity.ok(testId);
    }

    /**
     * 停止正在进行的测试
     */
    @PostMapping("/stop/{testId}")
    public ResponseEntity<Void> stopTest(@PathVariable String testId) {
        fixTestService.stopTest(testId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取测试状态
     */
    @GetMapping("/status/{testId}")
    public ResponseEntity<String> getTestStatus(@PathVariable String testId) {
        String status = fixTestService.getTestStatus(testId);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取测试结果
     */
    @GetMapping("/result/{testId}")
    public ResponseEntity<TestResultDTO> getTestResult(@PathVariable String testId) {
        TestResultDTO result = fixTestService.getTestResult(testId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取历史测试记录
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTestHistory() {
        return ResponseEntity.ok(fixTestService.getTestHistory());
    }
}
    