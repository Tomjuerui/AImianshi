package org.itjuerui.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 创建面试请求
 */
@Data
public class InterviewCreateRequest {
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长必须大于0")
    private Integer durationMinutes; // 30, 45, 60
}
