package org.itjuerui.service;

/**
 * 报告服务接口
 */
public interface ReportService {
    /**
     * 获取报告
     */
    Object getReport(Long sessionId);

    /**
     * 导出报告
     */
    Object exportReport(Long sessionId, String format);
}
