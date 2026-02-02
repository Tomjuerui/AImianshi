package org.itjuerui.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itjuerui.common.exception.BusinessException;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.entity.InterviewTurn;
import org.itjuerui.domain.interview.enums.SessionStatus;
import org.itjuerui.domain.interview.enums.TurnRole;
import org.itjuerui.domain.report.entity.Report;
import org.itjuerui.infra.repo.InterviewSessionMapper;
import org.itjuerui.infra.repo.InterviewTurnMapper;
import org.itjuerui.infra.repo.ReportMapper;
import org.itjuerui.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final ReportMapper reportMapper;

    @Override
    public Report getReport(Long sessionId) {
        Report report = getReportBySessionId(sessionId);
        if (report == null) {
            throw new BusinessException("报告不存在: " + sessionId);
        }
        log.info("获取报告: sessionId={}", sessionId);
        return report;
    }


    @Override
    @Transactional
    public Report generateReport(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在: " + sessionId);
        }
        if (session.getStatus() != SessionStatus.ENDED) {
            throw new BusinessException("请先结束会话再生成报告");
        }

        List<InterviewTurn> turns = listTurns(sessionId);
        if (turns.isEmpty()) {
            throw new BusinessException("会话暂无对话内容，无法生成报告");
        }

        List<InterviewTurn> candidateTurns = filterCandidateTurns(turns);
        if (candidateTurns.isEmpty()) {
            throw new BusinessException("缺少候选人回答，无法生成报告");
        }

        Report report = buildReport(sessionId, candidateTurns);
        Report existing = getReportBySessionId(sessionId);
        if (existing == null) {
            report.setCreatedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());
            reportMapper.insert(report);
        } else {
            report.setId(existing.getId());
            report.setCreatedAt(existing.getCreatedAt());
            report.setUpdatedAt(LocalDateTime.now());
            reportMapper.updateById(report);
        }

        log.info("生成面试报告: sessionId={}, score={}", sessionId, report.getOverallScore());
        return report;
    }

    @Override
    public Object exportReport(Long sessionId, String format) {
        // TODO: 实现导出报告逻辑
        log.info("导出报告: sessionId={}, format={}", sessionId, format);
        return null;
    }


    private Report getReportBySessionId(Long sessionId) {
        LambdaQueryWrapper<Report> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Report::getSessionId, sessionId);
        return reportMapper.selectOne(queryWrapper);
    }


    private List<InterviewTurn> listTurns(Long sessionId) {
        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getCreatedAt);
        return turnMapper.selectList(queryWrapper);
    }


    private List<InterviewTurn> filterCandidateTurns(List<InterviewTurn> turns) {
        List<InterviewTurn> candidateTurns = new ArrayList<>();
        for (InterviewTurn turn : turns) {
            if (turn.getRole() == TurnRole.CANDIDATE) {
                candidateTurns.add(turn);
            }
        }
        return candidateTurns;
    }


    private Report buildReport(Long sessionId, List<InterviewTurn> candidateTurns) {
        int totalTurns = candidateTurns.size();
        int totalLength = candidateTurns.stream()
                .mapToInt(turn -> turn.getContentText() == null ? 0 : turn.getContentText().length())
                .sum();
        int averageLength = totalLength / Math.max(1, totalTurns);

        int score = 50;
        score += Math.min(20, totalTurns * 5);
        score += Math.min(30, averageLength / 2);
        score = Math.max(0, Math.min(100, score));

        Report report = new Report();
        report.setSessionId(sessionId);
        report.setOverallScore(score);
        report.setSummary(String.format("候选人共回答%d次，平均回答长度约%d字，综合评分为%d分。",
                totalTurns, averageLength, score));
        report.setStrengths(JSON.toJSONString(buildStrengths(totalTurns, averageLength)));
        report.setWeaknesses(JSON.toJSONString(buildWeaknesses(totalTurns, averageLength)));
        report.setSuggestions(JSON.toJSONString(buildSuggestions(averageLength)));
        return report;
    }


    private List<String> buildStrengths(int totalTurns, int averageLength) {
        List<String> strengths = new ArrayList<>();
        if (averageLength >= 60) {
            strengths.add("回答内容较为充分，信息量充足");
        }
        if (totalTurns >= 3) {
            strengths.add("能够持续回应问题，沟通稳定");
        }
        if (strengths.isEmpty()) {
            strengths.add("回答态度积极，配合度良好");
        }
        return strengths;
    }


    private List<String> buildWeaknesses(int totalTurns, int averageLength) {
        List<String> weaknesses = new ArrayList<>();
        if (averageLength < 30) {
            weaknesses.add("回答较为简短，细节不足");
        }
        if (totalTurns < 2) {
            weaknesses.add("回答轮次偏少，信息覆盖有限");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("部分回答缺少结构化表达");
        }
        return weaknesses;
    }


    private List<String> buildSuggestions(int averageLength) {
        List<String> suggestions = new ArrayList<>();
        if (averageLength < 50) {
            suggestions.add("适当补充背景与细节，提升回答完整度");
        } else {
            suggestions.add("保持回答的清晰结构，并突出关键技术点");
        }
        suggestions.add("结合具体项目经验举例，增强说服力");
        return suggestions;
    }
}
