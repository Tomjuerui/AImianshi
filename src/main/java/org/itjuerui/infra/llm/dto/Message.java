package org.itjuerui.infra.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 消息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /**
     * 角色：user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
