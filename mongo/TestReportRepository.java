package com.finance.fix.tester.repository;

import com.finance.fix.tester.model.TestReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试报告数据访问接口，基于Spring Data MongoDB
 */
@Repository
public interface TestReportRepository extends MongoRepository<TestReport, String> {

    /**
     * 根据配置ID查询报告
     * @param configId 配置ID
     * @return 该配置的所有测试报告
     */
    List<TestReport> findByConfigIdOrderByStartTimeDesc(String configId);
    
    /**
     * 根据状态查询报告
     * @param status 测试状态
     * @return 该状态的所有测试报告
     */
    List<TestReport> findByStatusOrderByStartTimeDesc(String status);
    
    /**
     * 查询指定时间范围内的报告
     * @param start 开始时间
     * @param end 结束时间
     * @return 时间范围内的报告
     */
    List<TestReport> findByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime start, LocalDateTime end);
}
    