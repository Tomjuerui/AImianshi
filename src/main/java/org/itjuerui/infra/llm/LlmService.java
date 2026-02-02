package org.itjuerui.infra.llm;

import lombok.extern.slf4j.Slf4j;
import org.itjuerui.common.exception.BusinessException;
import org.itjuerui.infra.llm.dto.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM 服务封装
 */
@Slf4j
@Service
public class LlmService {

    private final ObjectProvider<LlmClient> clientProvider;

    public LlmService(ObjectProvider<LlmClient> clientProvider) {
        this.clientProvider = clientProvider;
    }


    /**
     * 调用 LLM 生成内容
     *
     * @param messages 消息列表
     * @return LLM 响应内容
     */
    public String chat(List<Message> messages) {
        LlmClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(500, "LLM 未配置");
        }
        try {
            return client.chat(messages);
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            throw new BusinessException(500, "LLM 调用失败");
        }
    }
}
