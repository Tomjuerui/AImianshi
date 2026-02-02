package org.itjuerui.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 报告 AI 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "report.ai")
public class ReportAiProperties {
    /**
     * 是否启用 AI 报告润色
     */
    private boolean enabled = false;
}
