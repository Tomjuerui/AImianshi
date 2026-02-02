package org.itjuerui.domain.interview.enums;

/**
 * 面试会话状态
 */
public enum SessionStatus {
    CREATED("已创建"),
    RUNNING("进行中"),
    ENDED("已结束"),
    FINISHED("已完成"),
    FAILED("失败");

    private final String description;

    SessionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
