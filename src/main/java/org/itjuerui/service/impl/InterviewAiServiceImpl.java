package org.itjuerui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itjuerui.common.exception.BusinessException;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.entity.InterviewTurn;
import org.itjuerui.domain.interview.enums.SessionStatus;
import org.itjuerui.domain.interview.enums.TurnRole;
import org.itjuerui.infra.llm.LlmService;
import org.itjuerui.infra.llm.dto.Message;
import org.itjuerui.infra.repo.InterviewSessionMapper;
import org.itjuerui.infra.repo.InterviewTurnMapper;
import org.itjuerui.service.InterviewAiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 面试 AI 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiServiceImpl implements InterviewAiService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final LlmService llmService;


    /**
     * 生成下一道面试问题并落库
     *
     * @param sessionId 会话ID
     * @return 新增的对话轮次
     */
    @Override
    @Transactional
    public InterviewTurn generateNextQuestion(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在: " + sessionId);
        }

        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getCreatedAt);
        List<InterviewTurn> turns = turnMapper.selectList(queryWrapper);

        List<Message> messages = buildMessages(turns);
        String question = llmService.chat(messages);
        if (question == null || question.trim().isEmpty()) {
            throw new BusinessException(500, "LLM 返回空问题");
        }

        if (session.getStatus() == SessionStatus.CREATED) {
            session.setStatus(SessionStatus.RUNNING);
            session.setStartedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }

        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(sessionId);
        turn.setRole(TurnRole.INTERVIEWER);
        turn.setContentText(question.trim());
        turn.setCreatedAt(LocalDateTime.now());
        turnMapper.insert(turn);

        log.info("生成下一道面试问题: sessionId={}, turnId={}", sessionId, turn.getId());
        return turn;
    }


    private List<Message> buildMessages(List<InterviewTurn> turns) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system",
                "你是资深Java后端面试官。请根据候选人与面试官的历史对话提出下一道问题，一次只问一个问题，只输出问题文本。"));

        StringBuilder historyBuilder = new StringBuilder();
        if (turns.isEmpty()) {
            historyBuilder.append("暂无历史对话，请提出第一道面试问题。");
        } else {
            historyBuilder.append("历史对话如下：\n");
            for (InterviewTurn turn : turns) {
                historyBuilder.append(formatRole(turn.getRole()))
                        .append("：")
                        .append(turn.getContentText())
                        .append("\n");
            }
        }
        messages.add(new Message("user", historyBuilder.toString()));
        return messages;
    }


    private String formatRole(TurnRole role) {
        if (role == TurnRole.CANDIDATE) {
            return "候选人";
        }
        return "面试官";
    }
}
