package org.itjuerui.domain.interview.enums;

/**
 * 对话角色
 */
public enum TurnRole {
    INTERVIEWER("面试官"),
    CANDIDATE("候选人"),
    SYSTEM("系统");

    private final String description;

    TurnRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
