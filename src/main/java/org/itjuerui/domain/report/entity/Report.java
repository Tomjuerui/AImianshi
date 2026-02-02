package org.itjuerui.domain.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试报告实体
 */
@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer totalScore;
    private String grade;
    private String dimensionScoresJson;
    private String strengthsJson;
    private String issuesJson;
    private String knowledgeGapsJson;
    private String nextActionsJson;
    private String reportJson;
    private String pdfUrl;
    private String mdUrl;
    private LocalDateTime createdAt;
}
