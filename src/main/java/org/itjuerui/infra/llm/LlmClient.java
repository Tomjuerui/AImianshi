package org.itjuerui.infra.llm;

import org.itjuerui.infra.llm.dto.Message;
import java.util.List;

/**
 * LLM 客户端接口
 */
public interface LlmClient {
    /**
     * 聊天接口
     *
     * @param messages 消息列表
     * @return LLM 响应内容
     */
    String chat(List<Message> messages);
}
