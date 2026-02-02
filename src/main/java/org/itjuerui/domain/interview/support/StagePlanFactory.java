package org.itjuerui.domain.interview.support;

import org.itjuerui.domain.interview.dto.StagePlanStage;
import org.itjuerui.domain.interview.enums.InterviewStage;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认阶段计划构建器
 */
public final class StagePlanFactory {

    private StagePlanFactory() {
    }


    /**
     * 默认阶段计划
     *
     * @return 阶段列表
     */
    public static List<StagePlanStage> defaultStages() {
        List<StagePlanStage> stages = new ArrayList<>();
        stages.add(buildStage(InterviewStage.BASICS, "基础沟通",
                "确认候选人背景与岗位匹配度，了解基础能力与简历事实", 2));
        stages.add(buildStage(InterviewStage.PROJECT, "项目深挖",
                "深入了解候选人核心项目职责、技术选型与贡献", 3));
        stages.add(buildStage(InterviewStage.FUNDAMENTALS, "原理基础",
                "考察Java后端核心原理与基础知识掌握情况", 3));
        stages.add(buildStage(InterviewStage.SCENARIOS, "场景设计",
                "评估候选人系统设计与场景问题解决能力", 2));
        return stages;
    }


    private static StagePlanStage buildStage(InterviewStage stage, String name, String goal, int minTurns) {
        StagePlanStage planStage = new StagePlanStage();
        planStage.setCode(stage.name());
        planStage.setName(name);
        planStage.setGoal(goal);
        planStage.setMinTurns(minTurns);
        return planStage;
    }
}
