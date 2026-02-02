package org.itjuerui.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itjuerui.common.config.ReportAiProperties;
import org.itjuerui.common.exception.BusinessException;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.entity.InterviewTurn;
import org.itjuerui.domain.interview.enums.InterviewStage;
import org.itjuerui.domain.interview.enums.SessionStatus;
import org.itjuerui.domain.interview.enums.TurnRole;
import org.itjuerui.domain.report.entity.Report;
import org.itjuerui.domain.report.entity.StageMiniReport;
import org.itjuerui.infra.llm.LlmService;
import org.itjuerui.infra.llm.config.LlmProperties;
import org.itjuerui.infra.llm.dto.Message;
import org.itjuerui.infra.repo.InterviewSessionMapper;
import org.itjuerui.infra.repo.InterviewTurnMapper;
import org.itjuerui.infra.repo.ReportMapper;
import org.itjuerui.infra.repo.StageMiniReportMapper;
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
    private final StageMiniReportMapper stageMiniReportMapper;
    private final LlmService llmService;
    private final LlmProperties llmProperties;
    private final ReportAiProperties reportAiProperties;

    @Override
    public Report getReport(Long sessionId) {
        Report report = getReportBySessionId(sessionId);
        if (report == null) {
            throw new BusinessException("报告不存在: " + sessionId);
        }
        if (report.getStageReportsJson() != null && !report.getStageReportsJson().isBlank()) {
            try {
                report.setStageReports(JSON.parseArray(report.getStageReportsJson(), StageMiniReport.class));
            } catch (Exception ex) {
                log.warn("解析阶段小结失败: {}", ex.getMessage());
            }
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
        report = enhanceReportIfEnabled(report, candidateTurns);
        report = attachStageMiniReports(report, sessionId);
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
    @Transactional
    public StageMiniReport generateStageMiniReport(Long sessionId, InterviewStage stage) {
        if (stage == null) {
            throw new BusinessException("阶段为空，无法生成小结");
        }
        List<InterviewTurn> turns = listStageCandidateTurns(sessionId, stage.name());
        StageMiniReport miniReport = buildStageMiniReport(sessionId, stage.name(), turns);
        miniReport = enhanceStageMiniReportIfEnabled(miniReport, turns);

        StageMiniReport existing = getStageMiniReport(sessionId, stage.name());
        if (existing == null) {
            miniReport.setCreatedAt(LocalDateTime.now());
            miniReport.setUpdatedAt(LocalDateTime.now());
            stageMiniReportMapper.insert(miniReport);
        } else {
            miniReport.setId(existing.getId());
            miniReport.setCreatedAt(existing.getCreatedAt());
            miniReport.setUpdatedAt(LocalDateTime.now());
            stageMiniReportMapper.updateById(miniReport);
        }
        return miniReport;
    }


    @Override
    public List<StageMiniReport> listStageMiniReports(Long sessionId) {
        LambdaQueryWrapper<StageMiniReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StageMiniReport::getSessionId, sessionId)
                .orderByAsc(StageMiniReport::getCreatedAt);
        return stageMiniReportMapper.selectList(queryWrapper);
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


    private StageMiniReport getStageMiniReport(Long sessionId, String stageCode) {
        LambdaQueryWrapper<StageMiniReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StageMiniReport::getSessionId, sessionId)
                .eq(StageMiniReport::getStageCode, stageCode);
        return stageMiniReportMapper.selectOne(queryWrapper);
    }


    private List<InterviewTurn> listTurns(Long sessionId) {
        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getCreatedAt);
        return turnMapper.selectList(queryWrapper);
    }


    private List<InterviewTurn> listStageCandidateTurns(Long sessionId, String stageCode) {
        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getRole, TurnRole.CANDIDATE)
                .eq(InterviewTurn::getStageCode, stageCode)
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


    private StageMiniReport buildStageMiniReport(Long sessionId, String stageCode, List<InterviewTurn> candidateTurns) {
        int totalTurns = candidateTurns.size();
        int totalLength = candidateTurns.stream()
                .mapToInt(turn -> turn.getContentText() == null ? 0 : turn.getContentText().length())
                .sum();
        int averageLength = totalLength / Math.max(1, totalTurns);

        int score = 40;
        score += Math.min(30, totalTurns * 8);
        score += Math.min(30, averageLength / 2);
        score = Math.max(0, Math.min(100, score));

        StageMiniReport report = new StageMiniReport();
        report.setSessionId(sessionId);
        report.setStageCode(stageCode);
        report.setScore(score);
        if (totalTurns == 0) {
            report.setSummary("该阶段候选人回答不足，信息有限。");
            report.setStrengths(JSON.toJSONString(List.of("信息不足，无法判断亮点")));
            report.setWeaknesses(JSON.toJSONString(List.of("缺少有效回答")));
            report.setSuggestions(JSON.toJSONString(List.of("补充该阶段的关键问题回答")));
            return report;
        }
        report.setSummary(String.format("该阶段候选人回答%d次，平均回答长度约%d字，阶段评分为%d分。",
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


    private Report enhanceReportIfEnabled(Report report, List<InterviewTurn> candidateTurns) {
        if (!reportAiProperties.isEnabled()) {
            report.setAiEnabled(false);
            return report;
        }

        List<Message> messages = buildAiMessages(report, candidateTurns);
        try {
            String response = llmService.chat(messages);
            ReportAiResult result = parseAiResult(response);
            if (result != null) {
                report.setSummary(result.getSummary());
                report.setStrengths(JSON.toJSONString(result.getStrengths()));
                report.setWeaknesses(JSON.toJSONString(result.getWeaknesses()));
                report.setSuggestions(JSON.toJSONString(result.getSuggestions()));
                report.setAiEnabled(true);
                report.setAiProvider(llmProperties.getProvider());
                report.setAiModel(llmProperties.getModel());
                return report;
            }
        } catch (Exception ex) {
            log.warn("AI 报告润色失败，已降级为规则版: {}", ex.getMessage());
        }

        report.setAiEnabled(false);
        return report;
    }


    private StageMiniReport enhanceStageMiniReportIfEnabled(StageMiniReport report, List<InterviewTurn> candidateTurns) {
        if (!reportAiProperties.isEnabled()) {
            return report;
        }

        List<Message> messages = buildStageAiMessages(report, candidateTurns);
        try {
            String response = llmService.chat(messages);
            ReportAiResult result = parseAiResult(response);
            if (result != null) {
                report.setSummary(result.getSummary());
                report.setStrengths(JSON.toJSONString(result.getStrengths()));
                report.setWeaknesses(JSON.toJSONString(result.getWeaknesses()));
                report.setSuggestions(JSON.toJSONString(result.getSuggestions()));
                return report;
            }
        } catch (Exception ex) {
            log.warn("阶段小结 AI 润色失败，已降级为规则版: {}", ex.getMessage());
        }
        return report;
    }


    private List<Message> buildAiMessages(Report report, List<InterviewTurn> candidateTurns) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system",
                "你是资深Java后端面试官，需要将规则版面试报告润色成更自然的面试点评。"
                        + "请严格输出JSON，字段包括 summary(字符串), strengths(字符串数组), weaknesses(字符串数组), suggestions(字符串数组)。"));

        StringBuilder userContent = new StringBuilder();
        userContent.append("规则评分信息：\n")
                .append("overallScore=").append(report.getOverallScore()).append("\n")
                .append("summary=").append(report.getSummary()).append("\n")
                .append("strengths=").append(report.getStrengths()).append("\n")
                .append("weaknesses=").append(report.getWeaknesses()).append("\n")
                .append("suggestions=").append(report.getSuggestions()).append("\n");

        int totalTurns = candidateTurns.size();
        int totalLength = candidateTurns.stream()
                .mapToInt(turn -> turn.getContentText() == null ? 0 : turn.getContentText().length())
                .sum();
        int averageLength = totalLength / Math.max(1, totalTurns);

        userContent.append("候选人回答统计：回答次数=")
                .append(totalTurns)
                .append("，平均长度=")
                .append(averageLength)
                .append("。\n")
                .append("对话摘录：\n")
                .append(buildTurnSnippet(candidateTurns));

        messages.add(new Message("user", userContent.toString()));
        return messages;
    }


    private List<Message> buildStageAiMessages(StageMiniReport report, List<InterviewTurn> candidateTurns) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system",
                "你是资深Java后端面试官，请润色阶段小结为更自然的面试点评。"
                        + "请严格输出JSON，字段包括 summary(字符串), strengths(字符串数组), weaknesses(字符串数组), suggestions(字符串数组)。"));

        StringBuilder userContent = new StringBuilder();
        userContent.append("阶段小结规则信息：\n")
                .append("stageCode=").append(report.getStageCode()).append("\n")
                .append("score=").append(report.getScore()).append("\n")
                .append("summary=").append(report.getSummary()).append("\n")
                .append("strengths=").append(report.getStrengths()).append("\n")
                .append("weaknesses=").append(report.getWeaknesses()).append("\n")
                .append("suggestions=").append(report.getSuggestions()).append("\n");

        userContent.append("对话摘录：\n")
                .append(buildTurnSnippet(candidateTurns));
        messages.add(new Message("user", userContent.toString()));
        return messages;
    }


    private String buildTurnSnippet(List<InterviewTurn> candidateTurns) {
        int startIndex = Math.max(0, candidateTurns.size() - 3);
        StringBuilder snippet = new StringBuilder();
        for (int i = startIndex; i < candidateTurns.size(); i++) {
            InterviewTurn turn = candidateTurns.get(i);
            snippet.append("候选人：").append(turn.getContentText()).append("\n");
        }
        return snippet.toString();
    }


    private ReportAiResult parseAiResult(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(response, ReportAiResult.class);
        } catch (Exception ex) {
            log.warn("AI 报告 JSON 解析失败: {}", ex.getMessage());
            return null;
        }
    }


    private Report attachStageMiniReports(Report report, Long sessionId) {
        List<StageMiniReport> stageReports = listStageMiniReports(sessionId);
        if (stageReports.isEmpty()) {
            return report;
        }
        report.setStageReports(stageReports);
        report.setStageReportsJson(JSON.toJSONString(stageReports));

        StringBuilder summaryBuilder = new StringBuilder(report.getSummary());
        summaryBuilder.append(" 分阶段评价：");
        for (StageMiniReport stageReport : stageReports) {
            summaryBuilder.append(stageReport.getStageCode())
                    .append(" ")
                    .append(stageReport.getSummary())
                    .append("；");
        }
        report.setSummary(summaryBuilder.toString());

        List<String> strengths = mergeStringLists(report.getStrengths(), stageReports, StageMiniReport::getStrengths);
        List<String> weaknesses = mergeStringLists(report.getWeaknesses(), stageReports, StageMiniReport::getWeaknesses);
        List<String> suggestions = mergeStringLists(report.getSuggestions(), stageReports, StageMiniReport::getSuggestions);

        report.setStrengths(JSON.toJSONString(strengths));
        report.setWeaknesses(JSON.toJSONString(weaknesses));
        report.setSuggestions(JSON.toJSONString(suggestions));
        return report;
    }


    private List<String> mergeStringLists(String baseJson, List<StageMiniReport> stageReports,
            java.util.function.Function<StageMiniReport, String> extractor) {
        List<String> merged = parseStringList(baseJson);
        for (StageMiniReport stageReport : stageReports) {
            merged.addAll(parseStringList(extractor.apply(stageReport)));
        }
        return merged;
    }


    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception ex) {
            List<String> fallback = new ArrayList<>();
            fallback.add(json);
            return fallback;
        }
    }


    private static class ReportAiResult {
        private String summary;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> suggestions;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }
}
