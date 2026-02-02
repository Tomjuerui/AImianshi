package org.itjuerui.api.dto;

import lombok.Data;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.entity.InterviewTurn;

import java.util.List;

/**
 * 会话详情响应
 */
@Data
public class SessionDetailResponse {
    private InterviewSession session;
    private List<InterviewTurn> turns;
}
