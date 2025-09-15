package com.finance.fix.tester.service;

import com.finance.fix.tester.model.TestReport;
import com.finance.fix.tester.repository.TestReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 测试报告服务类，处理报告的CRUD和业务逻辑
 */
@Service
public class TestReportService {

    private final TestReportRepository reportRepository;

    @Autowired
    public TestReportService(TestReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * 创建新的测试报告
     * @param report 测试报告对象
     * @return 保存后的测试报告
     */
    public TestReport createReport(TestReport report) {
        return reportRepository.save(report);
    }

    /**
     * 更新测试报告
     * @param report 测试报告对象
     * @return 更新后的测试报告
     */
    public TestReport updateReport(TestReport report) {
        return reportRepository.save(report);
    }

    /**
     * 获取所有测试报告
     * @return 所有测试报告列表
     */
    public List<TestReport> getAllReports() {
        return reportRepository.findAll();
    }

    /**
     * 根据ID获取测试报告
     * @param id 报告ID
     * @return 测试报告
     */
    public Optional<TestReport> getReportById(String id) {
        return reportRepository.findById(id);
    }

    /**
     * 根据配置ID获取测试报告
     * @param configId 配置ID
     * @return 该配置的所有测试报告
     */
    public List<TestReport> getReportsByConfigId(String configId) {
        return reportRepository.findByConfigIdOrderByStartTimeDesc(configId);
    }

    /**
     * 根据状态获取测试报告
     * @param status 测试状态
     * @return 该状态的所有测试报告
     */
    public List<TestReport> getReportsByStatus(String status) {
        return reportRepository.findByStatusOrderByStartTimeDesc(status);
    }

    /**
     * 获取指定时间范围内的测试报告
     * @param start 开始时间
     * @param end 结束时间
     * @return 时间范围内的测试报告
     */
    public List<TestReport> getReportsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return reportRepository.findByStartTimeBetweenOrderByStartTimeDesc(start, end);
    }

    /**
     * 删除测试报告
     * @param id 报告ID
     */
    public void deleteReport(String id) {
        reportRepository.deleteById(id);
    }
}
    