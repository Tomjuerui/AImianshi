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
     * 生成阶段小结
     */
    org.itjuerui.domain.report.entity.StageMiniReport generateStageMiniReport(Long sessionId,
            org.itjuerui.domain.interview.enums.InterviewStage stage);


    /**
     * 查询阶段小结列表
     */
    java.util.List<org.itjuerui.domain.report.entity.StageMiniReport> listStageMiniReports(Long sessionId);

    /**
     * 导出报告
     */
    Object exportReport(Long sessionId, String format);
}
