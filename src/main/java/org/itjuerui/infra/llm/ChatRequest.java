package org.itjuerui.infra.llm;

import lombok.Data;
import java.util.List;

/**
 * LLM聊天请求
 */
@Data
public class ChatRequest {
    private String systemPrompt;
    private List<Message> messages;
    private Double temperature;
    private Integer maxTokens;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
