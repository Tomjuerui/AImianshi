package org.itjuerui.domain.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阶段小结实体
 */
@Data
@TableName("stage_mini_report")
public class StageMiniReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String stageCode;
    private Integer score;
    private String summary;
    private String strengths;
    private String weaknesses;
    private String suggestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
