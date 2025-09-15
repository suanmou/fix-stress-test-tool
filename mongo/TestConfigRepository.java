package com.finance.fix.tester.repository;

import com.finance.fix.tester.model.TestConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 测试配置数据访问接口，基于Spring Data MongoDB
 */
@Repository
public interface TestConfigRepository extends MongoRepository<TestConfig, String> {

    /**
     * 根据配置名称查询配置
     * @param name 配置名称
     * @return 匹配的配置列表
     */
    List<TestConfig> findByNameContainingIgnoreCase(String name);
    
    /**
     * 查询默认配置
     * @return 默认配置
     */
    Optional<TestConfig> findByIsDefaultTrue();
    
    /**
     * 根据创建者查询配置
     * @param createdBy 创建者
     * @return 匹配的配置列表
     */
    List<TestConfig> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
    