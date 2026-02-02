package org.itjuerui.infra.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    /**
     * LLM 提供商：qwen 或 deepseek
     */
    private String provider;

    /**
     * API Key（通用配置，也可通过 provider 特定配置）
     */
    private String apiKey;

    /**
     * Base URL（通用配置，也可通过 provider 特定配置）
     */
    private String baseUrl;

    /**
     * 模型名称
     */
    private String model;

    /**
     * Qwen 特定配置
     */
    private QwenConfig qwen = new QwenConfig();

    /**
     * DeepSeek 特定配置
     */
    private DeepseekConfig deepseek = new DeepseekConfig();

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }

    @Data
    public static class DeepseekConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }
}
