package com.finance.fix.tester.controller;

import com.finance.fix.tester.model.TestConfig;
import com.finance.fix.tester.service.TestConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 测试配置API控制器，提供配置管理的REST接口
 */
@RestController
@RequestMapping("/api/test-configs")
public class TestConfigController {

    private final TestConfigService configService;

    @Autowired
    public TestConfigController(TestConfigService configService) {
        this.configService = configService;
    }

    /**
     * 创建新的测试配置
     * @param config 测试配置
     * @return 创建的配置
     */
    @PostMapping
    public ResponseEntity<TestConfig> createConfig(@RequestBody TestConfig config) {
        TestConfig createdConfig = configService.createConfig(config);
        return new ResponseEntity<>(createdConfig, HttpStatus.CREATED);
    }

    /**
     * 更新测试配置
     * @param id 配置ID
     * @param config 更新后的配置
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<TestConfig> updateConfig(
            @PathVariable String id,
            @RequestBody TestConfig config) {
        try {
            TestConfig updatedConfig = configService.updateConfig(id, config);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取所有测试配置
     * @return 配置列表
     */
    @GetMapping
    public ResponseEntity<List<TestConfig>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    /**
     * 根据ID获取测试配置
     * @param id 配置ID
     * @return 配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestConfig> getConfigById(@PathVariable String id) {
        Optional<TestConfig> config = configService.getConfigById(id);
        return config.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 搜索配置
     * @param name 配置名称
     * @return 匹配的配置列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<TestConfig>> searchConfigs(@RequestParam String name) {
        return ResponseEntity.ok(configService.searchConfigsByName(name));
    }

    /**
     * 获取默认配置
     * @return 默认配置
     */
    @GetMapping("/default")
    public ResponseEntity<TestConfig> getDefaultConfig() {
        Optional<TestConfig> defaultConfig = configService.getDefaultConfig();
        return defaultConfig.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 删除配置
     * @param id 配置ID
     * @return 响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        try {
            configService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 复制配置
     * @param id 配置ID
     * @param newName 新配置名称
     * @return 新配置
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TestConfig> duplicateConfig(
            @PathVariable String id,
            @RequestParam String newName) {
        try {
            TestConfig newConfig = configService.duplicateConfig(id, newName);
            return new ResponseEntity<>(newConfig, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
    