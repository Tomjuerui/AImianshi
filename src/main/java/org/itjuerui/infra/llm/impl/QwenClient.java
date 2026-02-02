package org.itjuerui.infra.llm.impl;

import lombok.extern.slf4j.Slf4j;
import org.itjuerui.infra.llm.ChatRequest;
import org.itjuerui.infra.llm.ChatResponse;
import org.itjuerui.infra.llm.LLMClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 千问客户端实现
 */
@Slf4j
@Component
public class QwenClient implements LLMClient {

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        // TODO: 实现千问流式调用
        log.info("Qwen流式调用: {}", request);
        return Flux.just("这是模拟的流式响应");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        // TODO: 实现千问非流式调用
        log.info("Qwen非流式调用: {}", request);
        ChatResponse response = new ChatResponse();
        response.setContent("这是模拟的响应");
        return response;
    }
}
