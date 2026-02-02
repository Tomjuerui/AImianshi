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
     * 问题轮次（第几轮）
     */
    private Integer turnNumber;
}
