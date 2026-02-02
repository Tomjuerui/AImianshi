package org.itjuerui.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.itjuerui.api.dto.InterviewCreateRequest;
import org.itjuerui.api.dto.NextQuestionResponse;
import org.itjuerui.api.dto.SessionDetailResponse;
import org.itjuerui.api.dto.SessionListRequest;
import org.itjuerui.api.dto.SessionListResponse;
import org.itjuerui.api.dto.TurnRequest;
import org.itjuerui.common.dto.ApiResponse;
import org.itjuerui.service.InterviewService;
import org.itjuerui.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 面试管理控制器
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final ReportService reportService;

    /**
     * 创建面试会话
     */
    @PostMapping("/sessions")
    public ApiResponse<Long> createSession(@Valid @RequestBody InterviewCreateRequest request) {
        Long sessionId = interviewService.createInterview(request);
        return ApiResponse.success(sessionId);
    }

    /**
     * 追加对话轮次
     */
    @PostMapping("/sessions/{id}/turns")
    public ApiResponse<Long> addTurn(@PathVariable("id") Long sessionId, 
                                      @Valid @RequestBody TurnRequest request) {
        Long turnId = interviewService.addTurn(sessionId, request);
        return ApiResponse.success(turnId);
    }

    /**
     * 查询会话详情（包含 turns）
     */
    @GetMapping("/sessions/{id}")
    public ApiResponse<SessionDetailResponse> getSessionDetail(@PathVariable("id") Long sessionId) {
        SessionDetailResponse response = interviewService.getSessionDetail(sessionId);
        return ApiResponse.success(response);
    }

    /**
     * 查询会话列表（支持可选筛选 resumeId/status，支持分页）
     */
    @GetMapping("/sessions")
    public ApiResponse<SessionListResponse> getSessionList(
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        SessionListRequest request = new SessionListRequest();
        request.setResumeId(resumeId);
        if (status != null && !status.isEmpty()) {
            try {
                request.setStatus(org.itjuerui.domain.interview.enums.SessionStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态值，不设置筛选条件
            }
        }
        request.setPage(page);
        request.setSize(size);
        
        SessionListResponse response = interviewService.getSessionList(request);
        return ApiResponse.success(response);
    }

    /**
     * 获取下一个问题（占位实现）
     */
    @PostMapping("/sessions/{id}/next-question")
    public ApiResponse<NextQuestionResponse> getNextQuestion(@PathVariable("id") Long sessionId) {
        NextQuestionResponse response = interviewService.getNextQuestion(sessionId);
        return ApiResponse.success(response);
    }

    /**
     * 获取下一个问题（流式输出）
     */
    @GetMapping(value = "/sessions/{id}/next-question/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNextQuestion(@PathVariable("id") Long sessionId) {
        return interviewService.streamNextQuestion(sessionId);
    }

    /**
     * 结束面试会话
     */
    @PostMapping("/sessions/{id}/end")
    public ApiResponse<Long> endSession(@PathVariable("id") Long sessionId) {
        Long endedSessionId = interviewService.endSession(sessionId);
        return ApiResponse.success(endedSessionId);
    }

    /**
     * 推进到下一阶段
     */
    @PostMapping("/sessions/{id}/stage/next")
    public ApiResponse<?> advanceStage(@PathVariable("id") Long sessionId) {
        return ApiResponse.success(interviewService.advanceStage(sessionId));
    }

    /**
     * 生成面试报告
     */
    @PostMapping("/sessions/{id}/report")
    public ApiResponse<?> generateReport(@PathVariable("id") Long sessionId) {
        return ApiResponse.success(reportService.generateReport(sessionId));
    }

    /**
     * 查询面试报告
     */
    @GetMapping("/sessions/{id}/report")
    public ApiResponse<?> getReport(@PathVariable("id") Long sessionId) {
        return ApiResponse.success(reportService.getReport(sessionId));
    }

    // ========== 以下为原有接口，保留兼容性 ==========

    @PostMapping("/interviews")
    public ApiResponse<Long> createInterview(@Valid @RequestBody InterviewCreateRequest request) {
        Long sessionId = interviewService.createInterview(request);
        return ApiResponse.success(sessionId);
    }

    @PostMapping("/interviews/{sessionId}/start")
    public ApiResponse<?> startInterview(@PathVariable Long sessionId) {
        return ApiResponse.success(interviewService.startInterview(sessionId));
    }

    @PostMapping("/interviews/{sessionId}/turn")
    public ApiResponse<?> submitTurn(@PathVariable Long sessionId, @RequestBody TurnRequest request) {
        return ApiResponse.success(interviewService.submitTurn(sessionId, request.getContent()));
    }

    @GetMapping(value = "/interviews/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ApiResponse<?> streamInterview(@PathVariable Long sessionId, @RequestParam String candidateText) {
        // SSE 流式输出实现
        return ApiResponse.success(interviewService.streamResponse(sessionId, candidateText));
    }

    @PostMapping("/interviews/{sessionId}/finish")
    public ApiResponse<?> finishInterview(@PathVariable Long sessionId) {
        interviewService.finishInterview(sessionId);
        return ApiResponse.success();
    }
}
