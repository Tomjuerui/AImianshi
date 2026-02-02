package org.itjuerui.infra.llm.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.itjuerui.infra.llm.LlmClient;
import org.itjuerui.infra.llm.config.LlmProperties;
import org.itjuerui.infra.llm.dto.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek LLM 客户端实现
 * 通过 HTTP 调用 DeepSeek API
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek", matchIfMissing = false)
public class DeepseekLlmClient implements LlmClient {

    private RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepseekLlmClient(LlmProperties properties) {
        // 优先使用 provider 特定配置，否则使用通用配置
        LlmProperties.DeepseekConfig deepseekConfig = properties.getDeepseek();
        this.apiKey = (deepseekConfig.getApiKey() != null && !deepseekConfig.getApiKey().isEmpty())
                ? deepseekConfig.getApiKey()
                : properties.getApiKey();
        this.baseUrl = (deepseekConfig.getBaseUrl() != null && !deepseekConfig.getBaseUrl().isEmpty())
                ? deepseekConfig.getBaseUrl()
                : properties.getBaseUrl();
        this.model = (deepseekConfig.getModel() != null && !deepseekConfig.getModel().isEmpty())
                ? deepseekConfig.getModel()
                : properties.getModel();

        // 不在此处验证配置，允许应用启动
        // 配置验证将在调用时进行
        log.info("DeepseekLlmClient 初始化完成（配置将在调用时验证）");
    }

    /**
     * 获取或创建 RestClient
     */
    private RestClient getRestClient() {
        if (restClient == null) {
            if (this.baseUrl == null || this.baseUrl.isEmpty()) {
                throw new IllegalStateException("DeepSeek Base URL 未配置，请在 application.properties 中设置 llm.baseUrl 或 llm.deepseek.baseUrl");
            }
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                throw new IllegalStateException("DeepSeek API Key 未配置，请在 application.properties 中设置 llm.apiKey 或 llm.deepseek.apiKey");
            }
            restClient = RestClient.builder()
                    .baseUrl(this.baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                    .build();
        }
        return restClient;
    }

    @Override
    public String chat(List<Message> messages) {
        // 验证配置（调用时检查，允许应用启动）
        if (this.model == null || this.model.isEmpty()) {
            throw new IllegalStateException("DeepSeek Model 未配置，请在 application.properties 中设置 llm.model 或 llm.deepseek.model");
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", this.model);
            
            // 转换消息格式
            List<Map<String, String>> messageList = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                messageList.add(messageMap);
            }
            requestBody.put("messages", messageList);

            log.debug("DeepSeek API 请求: {}", JSON.toJSONString(requestBody));

            // 发送请求（getRestClient 会检查 apiKey 和 baseUrl）
            String responseBody = getRestClient().post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("DeepSeek API 响应: {}", responseBody);

            // 解析响应
            JSONObject responseJson = JSON.parseObject(responseBody);
            if (responseJson.containsKey("choices") && responseJson.getJSONArray("choices").size() > 0) {
                JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getString("content");
            } else {
                throw new IllegalStateException("DeepSeek API 响应格式异常: " + responseBody);
            }
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            throw new IllegalStateException("调用 DeepSeek API 失败: " + e.getMessage(), e);
        }
    }
}
