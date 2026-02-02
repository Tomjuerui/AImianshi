package org.itjuerui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itjuerui.api.dto.InterviewCreateRequest;
import org.itjuerui.api.dto.NextQuestionResponse;
import org.itjuerui.api.dto.SessionDetailResponse;
import org.itjuerui.api.dto.SessionListRequest;
import org.itjuerui.api.dto.SessionListResponse;
import org.itjuerui.api.dto.TurnRequest;
import org.itjuerui.common.exception.BusinessException;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.entity.InterviewTurn;
import org.itjuerui.domain.interview.enums.SessionStatus;
import org.itjuerui.domain.interview.enums.TurnRole;
import org.itjuerui.infra.repo.InterviewSessionMapper;
import org.itjuerui.infra.repo.InterviewTurnMapper;
import org.itjuerui.service.InterviewAiService;
import org.itjuerui.service.InterviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final InterviewAiService interviewAiService;

    @Override
    @Transactional
    public Long createInterview(InterviewCreateRequest request) {
        InterviewSession session = new InterviewSession();
        session.setUserId(1L); // TODO: 从上下文获取当前用户ID
        session.setResumeId(request.getResumeId());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setStatus(SessionStatus.CREATED);
        session.setCreatedAt(LocalDateTime.now());

        sessionMapper.insert(session);
        log.info("创建面试会话: sessionId={}, resumeId={}, duration={}",
                session.getId(), request.getResumeId(), request.getDurationMinutes());
        return session.getId();
    }

    @Override
    @Transactional
    public Long addTurn(Long sessionId, TurnRequest request) {
        // 检查会话是否存在
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在: " + sessionId);
        }

        // 解析角色
        TurnRole role;
        try {
            role = TurnRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色: " + request.getRole());
        }

        // 创建 turn
        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(sessionId);
        turn.setRole(role);
        turn.setContentText(request.getContent());
        turn.setCreatedAt(LocalDateTime.now());

        turnMapper.insert(turn);
        log.info("添加对话轮次: turnId={}, sessionId={}, role={}",
                turn.getId(), sessionId, role);
        return turn.getId();
    }

    @Override
    public SessionDetailResponse getSessionDetail(Long sessionId) {
        // 查询会话
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在: " + sessionId);
        }

        // 查询该会话的所有 turns，按创建时间排序
        LambdaQueryWrapper<InterviewTurn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getCreatedAt);
        List<InterviewTurn> turns = turnMapper.selectList(queryWrapper);

        SessionDetailResponse response = new SessionDetailResponse();
        response.setSession(session);
        response.setTurns(turns);

        log.info("查询会话详情: sessionId={}, turnsCount={}", sessionId, turns.size());
        return response;
    }

    @Override
    public SessionListResponse getSessionList(SessionListRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<InterviewSession> queryWrapper = new LambdaQueryWrapper<>();

        // 可选筛选：resumeId
        if (request.getResumeId() != null) {
            queryWrapper.eq(InterviewSession::getResumeId, request.getResumeId());
        }

        // 可选筛选：status
        if (request.getStatus() != null) {
            queryWrapper.eq(InterviewSession::getStatus, request.getStatus());
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(InterviewSession::getCreatedAt);

        // 分页参数
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 10;

        // 执行分页查询
        Page<InterviewSession> pageParam = new Page<>(page, size);
        IPage<InterviewSession> pageResult = sessionMapper.selectPage(pageParam, queryWrapper);

        // 构建响应
        SessionListResponse response = new SessionListResponse();
        response.setSessions(pageResult.getRecords());
        response.setTotal(pageResult.getTotal());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages((int) pageResult.getPages());

        log.info("查询会话列表: resumeId={}, status={}, page={}, size={}, total={}",
                request.getResumeId(), request.getStatus(), page, size, pageResult.getTotal());
        return response;
    }

    @Override
    @Transactional
    public NextQuestionResponse getNextQuestion(Long sessionId) {
        InterviewTurn turn = interviewAiService.generateNextQuestion(sessionId);

        NextQuestionResponse response = new NextQuestionResponse();
        response.setQuestion(turn.getContentText());
        response.setTurnId(turn.getId());

        log.info("生成下一个问题: sessionId={}, turnId={}", sessionId, turn.getId());
        return response;
    }

    @Override
    public Object startInterview(Long sessionId) {
        // TODO: 实现开始面试逻辑
        log.info("开始面试: sessionId={}", sessionId);
        return null;
    }

    @Override
    public Object submitTurn(Long sessionId, String candidateText) {
        // TODO: 实现对话提交逻辑
        log.info("提交对话: sessionId={}, text={}", sessionId, candidateText);
        return null;
    }

    @Override
    public Object streamResponse(Long sessionId, String candidateText) {
        // TODO: 实现流式响应逻辑
        log.info("流式响应: sessionId={}, text={}", sessionId, candidateText);
        return null;
    }

    @Override
    public void finishInterview(Long sessionId) {
        // TODO: 实现结束面试逻辑
        log.info("结束面试: sessionId={}", sessionId);
    }
}
