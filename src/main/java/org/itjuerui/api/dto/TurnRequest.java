package org.itjuerui.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话请求
 */
@Data
public class TurnRequest {
    /**
     * 对话内容（可以是问题、答案或普通内容）
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 角色：INTERVIEWER（面试官）、CANDIDATE（候选人）、SYSTEM（系统）
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}
