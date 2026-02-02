package org.itjuerui.domain.interview.enums;

/**
 * 面试阶段枚举
 */
public enum InterviewStage {
    BASICS("基础沟通"),
    PROJECT("项目深挖"),
    FUNDAMENTALS("原理基础"),
    SCENARIOS("场景设计"),
    INTRO("项目总览"),
    JAVA_BASIC("Java基础"),
    SPRING("Spring体系"),
    DB_CACHE("数据库与缓存"),
    MIDDLEWARE("中间件"),
    SYSTEM_DESIGN("系统设计"),
    END("结束");

    private final String description;

    InterviewStage(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
