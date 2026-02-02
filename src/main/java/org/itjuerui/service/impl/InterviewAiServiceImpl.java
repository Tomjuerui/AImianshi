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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();


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
        if (session.getStatus() == SessionStatus.ENDED) {
            throw new BusinessException("会话已结束");
        }

        ensureSessionRunning(session);

        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getCreatedAt);
        List<InterviewTurn> turns = turnMapper.selectList(queryWrapper);

        List<Message> messages = buildMessages(turns);
        String question = llmService.chat(messages);
        if (question == null || question.trim().isEmpty()) {
            throw new BusinessException(500, "LLM 返回空问题");
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


    /**
     * 流式生成下一道面试问题并落库
     *
     * @param sessionId 会话ID
     * @return SSE 流
     */
    @Override
    public SseEmitter streamNextQuestion(Long sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        streamExecutor.submit(() -> {
            try {
                InterviewSession session = sessionMapper.selectById(sessionId);
                if (session == null) {
                    sendError(emitter, "会话不存在: " + sessionId);
                    return;
                }
                if (session.getStatus() == SessionStatus.ENDED) {
                    sendError(emitter, "会话已结束");
                    return;
                }

                ensureSessionRunning(session);

                LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                        .orderByAsc(InterviewTurn::getCreatedAt);
                List<InterviewTurn> turns = turnMapper.selectList(queryWrapper);

                List<Message> messages = buildMessages(turns);
                String question = llmService.chat(messages);
                if (question == null || question.trim().isEmpty()) {
                    sendError(emitter, "LLM 返回空问题");
                    return;
                }
                String trimmedQuestion = question.trim();

                InterviewTurn turn = persistTurn(session, trimmedQuestion);
                sendChunks(emitter, trimmedQuestion);
                sendDone(emitter, turn.getId(), trimmedQuestion);
                emitter.complete();
            } catch (BusinessException ex) {
                log.warn("SSE 业务异常: {}", ex.getMessage());
                sendError(emitter, ex.getMessage());
            } catch (Exception ex) {
                log.error("SSE 生成下一道面试问题失败", ex);
                sendError(emitter, "系统内部错误，请稍后重试");
            }
        });
        return emitter;
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


    private InterviewTurn persistTurn(InterviewSession session, String question) {
        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(session.getId());
        turn.setRole(TurnRole.INTERVIEWER);
        turn.setContentText(question);
        turn.setCreatedAt(LocalDateTime.now());
        turnMapper.insert(turn);
        return turn;
    }


    private void sendChunks(SseEmitter emitter, String question) throws Exception {
        List<String> chunks = splitIntoChunks(question, 8);
        for (String chunk : chunks) {
            emitter.send(SseEmitter.event().name("chunk").data(chunk));
            Thread.sleep(30L);
        }
    }


    private void sendDone(SseEmitter emitter, Long turnId, String question) throws Exception {
        String payload = "{\"turnId\":" + turnId + ",\"question\":\"" + escapeJson(question) + "\"}";
        emitter.send(SseEmitter.event().name("done").data(payload));
    }


    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (Exception ex) {
            log.warn("发送 SSE 错误事件失败", ex);
        } finally {
            emitter.complete();
        }
    }


    private List<String> splitIntoChunks(String text, int size) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }


    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    private void ensureSessionRunning(InterviewSession session) {
        if (session.getStatus() == SessionStatus.CREATED) {
            session.setStatus(SessionStatus.RUNNING);
            if (session.getStartedAt() == null) {
                session.setStartedAt(LocalDateTime.now());
            }
            sessionMapper.updateById(session);
        }
    }
}
