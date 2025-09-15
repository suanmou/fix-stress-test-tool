package com.finance.fix.tester.service;

import com.finance.fix.tester.model.TestConfig;
import com.finance.fix.tester.repository.TestConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 测试配置服务类，处理配置的CRUD和业务逻辑
 */
@Service
public class TestConfigService {

    private final TestConfigRepository configRepository;

    @Autowired
    public TestConfigService(TestConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 创建新的测试配置
     * @param config 测试配置对象
     * @return 保存后的测试配置
     */
    public TestConfig createConfig(TestConfig config) {
        // 如果设置为默认配置，需要将其他默认配置取消
        if (config.isDefault()) {
            Optional<TestConfig> defaultConfig = configRepository.findByIsDefaultTrue();
            defaultConfig.ifPresent(cfg -> {
                cfg.setDefault(false);
                cfg.updateTimestamp();
                configRepository.save(cfg);
            });
        }
        
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(config);
    }

    /**
     * 更新测试配置
     * @param id 配置ID
     * @param updatedConfig 更新后的配置信息
     * @return 更新后的配置
     */
    public TestConfig updateConfig(String id, TestConfig updatedConfig) {
        return configRepository.findById(id)
                .map(existingConfig -> {
                    // 更新基本信息
                    existingConfig.setName(updatedConfig.getName());
                    existingConfig.setDescription(updatedConfig.getDescription());
                    existingConfig.setCreatedBy(updatedConfig.getCreatedBy());
                    
                    // 更新测试参数
                    existingConfig.setSessions(updatedConfig.getSessions());
                    existingConfig.setRate(updatedConfig.getRate());
                    existingConfig.setMessages(updatedConfig.getMessages());
                    existingConfig.setDuration(updatedConfig.getDuration());
                    existingConfig.setTimeout(updatedConfig.getTimeout());
                    existingConfig.setConfigPath(updatedConfig.getConfigPath());
                    existingConfig.setDetailedLog(updatedConfig.isDetailedLog());
                    existingConfig.setRecordSystemMetrics(updatedConfig.isRecordSystemMetrics());
                    existingConfig.setExtraParams(updatedConfig.getExtraParams());
                    
                    // 处理默认配置逻辑
                    if (updatedConfig.isDefault()) {
                        Optional<TestConfig> defaultConfig = configRepository.findByIsDefaultTrue();
                        defaultConfig.ifPresent(cfg -> {
                            if (!cfg.getId().equals(id)) {
                                cfg.setDefault(false);
                                cfg.updateTimestamp();
                                configRepository.save(cfg);
                            }
                        });
                        existingConfig.setDefault(true);
                    } else {
                        existingConfig.setDefault(false);
                    }
                    
                    existingConfig.updateTimestamp();
                    return configRepository.save(existingConfig);
                })
                .orElseThrow(() -> new IllegalArgumentException("测试配置不存在: " + id));
    }

    /**
     * 获取所有测试配置
     * @return 所有测试配置列表
     */
    public List<TestConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 根据ID获取测试配置
     * @param id 配置ID
     * @return 测试配置
     */
    public Optional<TestConfig> getConfigById(String id) {
        return configRepository.findById(id);
    }

    /**
     * 根据名称搜索配置
     * @param name 配置名称(模糊匹配)
     * @return 匹配的配置列表
     */
    public List<TestConfig> searchConfigsByName(String name) {
        return configRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * 获取默认配置
     * @return 默认配置
     */
    public Optional<TestConfig> getDefaultConfig() {
        return configRepository.findByIsDefaultTrue();
    }

    /**
     * 删除测试配置
     * @param id 配置ID
     */
    public void deleteConfig(String id) {
        // 不能删除默认配置
        Optional<TestConfig> config = configRepository.findById(id);
        if (config.isPresent() && config.get().isDefault()) {
            throw new IllegalStateException("不能删除默认配置，请先设置其他配置为默认");
        }
        
        configRepository.deleteById(id);
    }

    /**
     * 复制配置
     * @param id 要复制的配置ID
     * @param newName 新配置名称
     * @return 复制后的新配置
     */
    public TestConfig duplicateConfig(String id, String newName) {
        return configRepository.findById(id)
                .map(existingConfig -> {
                    TestConfig newConfig = new TestConfig();
                    newConfig.setName(newName);
                    newConfig.setDescription("复制自: " + existingConfig.getName());
                    newConfig.setCreatedBy(existingConfig.getCreatedBy());
                    newConfig.setDefault(false); // 复制的配置不能是默认配置
                    
                    // 复制测试参数
                    newConfig.setSessions(existingConfig.getSessions());
                    newConfig.setRate(existingConfig.getRate());
                    newConfig.setMessages(existingConfig.getMessages());
                    newConfig.setDuration(existingConfig.getDuration());
                    newConfig.setTimeout(existingConfig.getTimeout());
                    newConfig.setConfigPath(existingConfig.getConfigPath());
                    newConfig.setDetailedLog(existingConfig.isDetailedLog());
                    newConfig.setRecordSystemMetrics(existingConfig.isRecordSystemMetrics());
                    newConfig.setExtraParams(existingConfig.getExtraParams());
                    
                    return configRepository.save(newConfig);
                })
                .orElseThrow(() -> new IllegalArgumentException("测试配置不存在: " + id));
    }
}
    