package org.itjuerui.domain.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.itjuerui.domain.interview.enums.InterviewStage;
import org.itjuerui.domain.interview.enums.SessionStatus;

import java.time.LocalDateTime;

/**
 * 面试会话实体
 */
@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Integer durationMinutes;
    private SessionStatus status;
    private InterviewStage currentStage;
    private String stagePlanJson;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
