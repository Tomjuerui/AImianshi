package org.itjuerui.api.dto;

import lombok.Data;
import org.itjuerui.domain.interview.enums.SessionStatus;

/**
 * 会话列表查询请求
 */
@Data
public class SessionListRequest {
    /**
     * 简历ID（可选）
     */
    private Long resumeId;

    /**
     * 会话状态（可选）
     */
    private SessionStatus status;

    /**
     * 页码，从1开始
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;
}
