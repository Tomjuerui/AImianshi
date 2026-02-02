package org.itjuerui.api.dto;

import lombok.Data;

/**
 * 下一个问题响应
 */
@Data
public class NextQuestionResponse {
    /**
     * 问题内容
     */
    private String question;

    /**
     * 新增的 turnId
     */
    private Long turnId;
}
