package org.itjuerui.service;

import org.itjuerui.api.dto.InterviewCreateRequest;
import org.itjuerui.api.dto.NextQuestionResponse;
import org.itjuerui.api.dto.SessionDetailResponse;
import org.itjuerui.api.dto.SessionListRequest;
import org.itjuerui.api.dto.SessionListResponse;
import org.itjuerui.api.dto.TurnRequest;

/**
 * 面试服务接口
 */
public interface InterviewService {
    /**
     * 创建面试会话
     */
    Long createInterview(InterviewCreateRequest request);

    /**
     * 追加一轮对话
     */
    Long addTurn(Long sessionId, TurnRequest request);

    /**
     * 查询会话详情（包含 turns）
     */
    SessionDetailResponse getSessionDetail(Long sessionId);

    /**
     * 查询会话列表（支持筛选和分页）
     */
    SessionListResponse getSessionList(SessionListRequest request);

    /**
     * 获取下一个问题
     * 根据历史对话生成面试官问题，并自动写入一条 role=INTERVIEWER 的 turn
     */
    NextQuestionResponse getNextQuestion(Long sessionId);


    /**
     * 流式获取下一个问题
     */
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNextQuestion(Long sessionId);


    /**
     * 结束面试会话
     */
    Long endSession(Long sessionId);


    /**
     * 推进到下一阶段
     */
    org.itjuerui.domain.interview.entity.InterviewSession advanceStage(Long sessionId);

    /**
     * 开始面试
     */
    Object startInterview(Long sessionId);

    /**
     * 提交一轮对话
     */
    Object submitTurn(Long sessionId, String candidateText);

    /**
     * 流式响应
     */
    Object streamResponse(Long sessionId, String candidateText);

    /**
     * 结束面试
     */
    void finishInterview(Long sessionId);
}
