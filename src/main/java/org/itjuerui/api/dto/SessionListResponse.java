package org.itjuerui.api.dto;

import lombok.Data;
import org.itjuerui.domain.interview.entity.InterviewSession;

import java.util.List;

/**
 * 会话列表响应
 */
@Data
public class SessionListResponse {
    /**
     * 会话列表（不包含 turns）
     */
    private List<InterviewSession> sessions;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer totalPages;
}
