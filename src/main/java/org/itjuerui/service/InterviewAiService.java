package org.itjuerui.service;

import org.itjuerui.domain.interview.entity.InterviewTurn;

/**
 * 面试 AI 服务接口
 */
public interface InterviewAiService {
    /**
     * 生成下一道面试问题并落库
     *
     * @param sessionId 会话ID
     * @return 新增的对话轮次
     */
    InterviewTurn generateNextQuestion(Long sessionId);


    /**
     * 流式生成下一道面试问题并落库
     *
     * @param sessionId 会话ID
     * @return SSE 流
     */
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNextQuestion(Long sessionId);
}
