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
    private Integer overallScore;
    private String summary;
    private String strengths;
    private String weaknesses;
    private String suggestions;
    private Boolean aiEnabled;
    private String aiProvider;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
