package org.itjuerui.infra.llm;

import lombok.Data;

/**
 * LLM聊天响应
 */
@Data
public class ChatResponse {
    private String content;
    private Integer tokenUsage;
}
