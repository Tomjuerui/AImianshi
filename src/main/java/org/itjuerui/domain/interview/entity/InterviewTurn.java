package org.itjuerui.domain.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.itjuerui.domain.interview.enums.TurnRole;

import java.time.LocalDateTime;

/**
 * 面试对话轮次实体
 */
@Data
@TableName("interview_turn")
public class InterviewTurn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private TurnRole role;
    private String contentText;
    private String stageCode;
    private String audioUrl;
    private String tokenUsage;
    private LocalDateTime createdAt;
}
