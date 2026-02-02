package org.itjuerui.service;

/**
 * 报告服务接口
 */
public interface ReportService {
    /**
     * 获取报告
     */
    org.itjuerui.domain.report.entity.Report getReport(Long sessionId);


    /**
     * 生成或更新报告
     */
    org.itjuerui.domain.report.entity.Report generateReport(Long sessionId);

    /**
     * 导出报告
     */
    Object exportReport(Long sessionId, String format);
}
