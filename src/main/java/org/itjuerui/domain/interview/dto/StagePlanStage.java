package org.itjuerui.domain.interview.dto;

import lombok.Data;

/**
 * 阶段计划节点
 */
@Data
public class StagePlanStage {
    private String code;
    private String name;
    private String goal;
    private Integer minTurns;
}
