package org.itjuerui.api.controller;

import com.alibaba.fastjson2.JSON;
import org.itjuerui.api.dto.InterviewCreateRequest;
import org.itjuerui.api.dto.NextQuestionResponse;
import org.itjuerui.api.dto.SessionDetailResponse;
import org.itjuerui.api.dto.SessionListResponse;
import org.itjuerui.api.dto.TurnRequest;
import org.itjuerui.common.dto.ApiResponse;
import org.itjuerui.domain.interview.entity.InterviewSession;
import org.itjuerui.domain.interview.enums.SessionStatus;
import org.itjuerui.domain.interview.enums.TurnRole;
import org.itjuerui.infra.llm.LlmService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 面试控制器集成测试
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmService llmService;

    @Test
    void testCreateSession_Success() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setResumeId(1L);
        request.setDurationMinutes(30);

        MvcResult result = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<Long> response = JSON.parseObject(responseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        assertNotNull(response.getData());
        assertTrue(response.getData() > 0);
    }

    @Test
    void testCreateSession_ValidationFailed() throws Exception {
        // 测试缺少必填字段
        InterviewCreateRequest request = new InterviewCreateRequest();
        // 不设置 resumeId 和 durationMinutes

        mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testAddTurn_Success() throws Exception {
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 添加 turn
        TurnRequest turnRequest = new TurnRequest();
        turnRequest.setContent("This is a test question");
        turnRequest.setRole("INTERVIEWER");

        MvcResult turnResult = mockMvc.perform(post("/api/interview/sessions/{id}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(turnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String turnResponseBody = turnResult.getResponse().getContentAsString();
        ApiResponse<Long> turnResponse = JSON.parseObject(turnResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        assertNotNull(turnResponse.getData());
        assertTrue(turnResponse.getData() > 0);
    }

    @Test
    void testAddTurn_ValidationFailed() throws Exception {
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 测试缺少必填字段
        TurnRequest turnRequest = new TurnRequest();
        // 不设置 content 和 role

        mockMvc.perform(post("/api/interview/sessions/{id}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(turnRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testGetSessionDetail_Success() throws Exception {
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 添加两个 turns
        TurnRequest turnRequest1 = new TurnRequest();
        turnRequest1.setContent("What is your experience with Java?");
        turnRequest1.setRole("INTERVIEWER");
        mockMvc.perform(post("/api/interview/sessions/{id}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(turnRequest1)))
                .andExpect(status().isOk());

        TurnRequest turnRequest2 = new TurnRequest();
        turnRequest2.setContent("I have 3 years of Java development experience.");
        turnRequest2.setRole("CANDIDATE");
        mockMvc.perform(post("/api/interview/sessions/{id}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(turnRequest2)))
                .andExpect(status().isOk());

        // 查询会话详情
        MvcResult detailResult = mockMvc.perform(get("/api/interview/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.session").exists())
                .andExpect(jsonPath("$.data.turns").exists())
                .andExpect(jsonPath("$.data.turns").isArray())
                .andExpect(jsonPath("$.data.turns.length()").value(2))
                .andReturn();

        String detailResponseBody = detailResult.getResponse().getContentAsString();
        ApiResponse<SessionDetailResponse> detailResponse = JSON.parseObject(
                detailResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionDetailResponse>>() {}
        );
        
        assertNotNull(detailResponse.getData());
        assertNotNull(detailResponse.getData().getSession());
        assertEquals(sessionId, detailResponse.getData().getSession().getId());
        assertEquals(SessionStatus.CREATED, detailResponse.getData().getSession().getStatus());
        assertEquals(2, detailResponse.getData().getTurns().size());
        assertEquals(TurnRole.INTERVIEWER, detailResponse.getData().getTurns().get(0).getRole());
        assertEquals("What is your experience with Java?", detailResponse.getData().getTurns().get(0).getContentText());
        assertEquals(TurnRole.CANDIDATE, detailResponse.getData().getTurns().get(1).getRole());
        assertEquals("I have 3 years of Java development experience.", detailResponse.getData().getTurns().get(1).getContentText());
    }

    @Test
    void testGetSessionDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/interview/sessions/{id}", 99999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testAddTurn_InvalidRole() throws Exception {
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 测试无效的角色
        TurnRequest turnRequest = new TurnRequest();
        turnRequest.setContent("Test content");
        turnRequest.setRole("INVALID_ROLE");

        mockMvc.perform(post("/api/interview/sessions/{id}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(turnRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetNextQuestion_FirstCall() throws Exception {
        Mockito.when(llmService.chat(anyList())).thenReturn("请简要介绍你对 Spring Boot 的理解。");
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 验证初始状态为 CREATED，turns 数为 0
        MvcResult initialDetailResult = mockMvc.perform(get("/api/interview/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andReturn();
        String initialDetailBody = initialDetailResult.getResponse().getContentAsString();
        ApiResponse<SessionDetailResponse> initialDetailResponse = JSON.parseObject(
                initialDetailBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionDetailResponse>>() {}
        );
        assertEquals(SessionStatus.CREATED, initialDetailResponse.getData().getSession().getStatus());
        assertEquals(0, initialDetailResponse.getData().getTurns().size());
        assertNull(initialDetailResponse.getData().getSession().getStartedAt());

        // 调用 next-question
        MvcResult nextQuestionResult = mockMvc.perform(post("/api/interview/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.question").exists())
                .andExpect(jsonPath("$.data.turnId").exists())
                .andReturn();

        String nextQuestionBody = nextQuestionResult.getResponse().getContentAsString();
        ApiResponse<NextQuestionResponse> nextQuestionResponse = JSON.parseObject(
                nextQuestionBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<NextQuestionResponse>>() {}
        );
        assertNotNull(nextQuestionResponse.getData());
        assertNotNull(nextQuestionResponse.getData().getQuestion());
        assertFalse(nextQuestionResponse.getData().getQuestion().isEmpty());
        assertNotNull(nextQuestionResponse.getData().getTurnId());

        // 验证调用后：状态变为 RUNNING，turns 数+1，最新 turn 为 INTERVIEWER
        MvcResult detailResult = mockMvc.perform(get("/api/interview/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andReturn();
        String detailBody = detailResult.getResponse().getContentAsString();
        ApiResponse<SessionDetailResponse> detailResponse = JSON.parseObject(
                detailBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionDetailResponse>>() {}
        );
        
        assertEquals(SessionStatus.RUNNING, detailResponse.getData().getSession().getStatus());
        assertNotNull(detailResponse.getData().getSession().getStartedAt());
        assertEquals(1, detailResponse.getData().getTurns().size());
        assertEquals(TurnRole.INTERVIEWER, detailResponse.getData().getTurns().get(0).getRole());
        assertNotNull(detailResponse.getData().getTurns().get(0).getContentText());
        assertFalse(detailResponse.getData().getTurns().get(0).getContentText().isEmpty());
    }

    @Test
    void testGetNextQuestion_MultipleCalls() throws Exception {
        Mockito.when(llmService.chat(anyList())).thenReturn("请解释 Java 中的 JVM 内存模型。", "说说你对 GC 调优的理解。");
        // 先创建会话
        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(createResponseBody, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {});
        Long sessionId = createResponse.getData();

        // 第一次调用 next-question
        mockMvc.perform(post("/api/interview/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.turnId").exists());

        // 验证第一次调用后 turns 数为 1
        MvcResult detailResult1 = mockMvc.perform(get("/api/interview/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andReturn();
        String detailBody1 = detailResult1.getResponse().getContentAsString();
        ApiResponse<SessionDetailResponse> detailResponse1 = JSON.parseObject(
                detailBody1, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionDetailResponse>>() {}
        );
        assertEquals(1, detailResponse1.getData().getTurns().size());
        assertEquals(TurnRole.INTERVIEWER, detailResponse1.getData().getTurns().get(0).getRole());

        // 第二次调用 next-question
        mockMvc.perform(post("/api/interview/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.turnId").exists());

        // 验证第二次调用后 turns 数+1，最新 turn 为 INTERVIEWER
        MvcResult detailResult2 = mockMvc.perform(get("/api/interview/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andReturn();
        String detailBody2 = detailResult2.getResponse().getContentAsString();
        ApiResponse<SessionDetailResponse> detailResponse2 = JSON.parseObject(
                detailBody2, 
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionDetailResponse>>() {}
        );
        
        assertEquals(2, detailResponse2.getData().getTurns().size());
        // 验证最新的 turn（最后一个）为 INTERVIEWER
        int lastIndex = detailResponse2.getData().getTurns().size() - 1;
        assertEquals(TurnRole.INTERVIEWER, detailResponse2.getData().getTurns().get(lastIndex).getRole());
        assertNotNull(detailResponse2.getData().getTurns().get(lastIndex).getContentText());
        assertFalse(detailResponse2.getData().getTurns().get(lastIndex).getContentText().isEmpty());
    }

    @Test
    void testGetSessionList_WithPagination() throws Exception {
        // 创建多个会话用于测试分页
        Long resumeId1 = 1L;
        Long resumeId2 = 2L;
        
        // 创建 3 个会话，resumeId 为 1
        createSessionAndGetId(resumeId1, 30);
        createSessionAndGetId(resumeId1, 45);
        createSessionAndGetId(resumeId1, 60);
        
        // 创建 2 个会话，resumeId 为 2
        createSessionAndGetId(resumeId2, 30);
        createSessionAndGetId(resumeId2, 45);

        // 测试分页查询：第1页，每页2条
        MvcResult result = mockMvc.perform(get("/api/interview/sessions")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessions").exists())
                .andExpect(jsonPath("$.data.sessions").isArray())
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<SessionListResponse> response = JSON.parseObject(
                responseBody,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionListResponse>>() {}
        );

        SessionListResponse listResponse = response.getData();
        assertNotNull(listResponse);
        assertNotNull(listResponse.getSessions());
        // 验证返回的会话列表不包含 turns
        for (InterviewSession session : listResponse.getSessions()) {
            assertNotNull(session);
            assertNotNull(session.getId());
        }
        // 验证分页信息
        assertEquals(1, listResponse.getPage());
        assertEquals(2, listResponse.getSize());
        assertTrue(listResponse.getTotal() >= 5); // 至少包含我们创建的5个会话
        assertTrue(listResponse.getTotalPages() >= 3); // 至少3页（5条记录，每页2条）
        // 验证第1页返回2条记录
        assertEquals(2, listResponse.getSessions().size());
    }

    @Test
    void testGetSessionList_WithFilters() throws Exception {
        Long resumeId1 = 1L;
        Long resumeId2 = 2L;
        
        // 创建不同 resumeId 和状态的会话
        createSessionAndGetId(resumeId1, 30); // CREATED
        createSessionAndGetId(resumeId1, 45); // CREATED
        
        // 创建 resumeId2 的会话
        createSessionAndGetId(resumeId2, 30); // CREATED

        // 测试按 resumeId 筛选
        MvcResult result1 = mockMvc.perform(get("/api/interview/sessions")
                        .param("resumeId", resumeId1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessions").exists())
                .andReturn();

        String responseBody1 = result1.getResponse().getContentAsString();
        ApiResponse<SessionListResponse> response1 = JSON.parseObject(
                responseBody1,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionListResponse>>() {}
        );

        SessionListResponse listResponse1 = response1.getData();
        assertNotNull(listResponse1);
        assertNotNull(listResponse1.getSessions());
        // 验证所有返回的会话都是指定的 resumeId
        for (InterviewSession session : listResponse1.getSessions()) {
            assertEquals(resumeId1, session.getResumeId());
        }
        assertTrue(listResponse1.getTotal() >= 2); // 至少包含我们创建的2个会话

        // 测试按 status 筛选
        MvcResult result2 = mockMvc.perform(get("/api/interview/sessions")
                        .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessions").exists())
                .andReturn();

        String responseBody2 = result2.getResponse().getContentAsString();
        ApiResponse<SessionListResponse> response2 = JSON.parseObject(
                responseBody2,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionListResponse>>() {}
        );

        SessionListResponse listResponse2 = response2.getData();
        assertNotNull(listResponse2);
        assertNotNull(listResponse2.getSessions());
        // 验证所有返回的会话都是 CREATED 状态
        for (InterviewSession session : listResponse2.getSessions()) {
            assertEquals(SessionStatus.CREATED, session.getStatus());
        }
        assertTrue(listResponse2.getTotal() >= 3); // 至少包含我们创建的3个会话

        // 测试组合筛选：resumeId + status
        MvcResult result3 = mockMvc.perform(get("/api/interview/sessions")
                        .param("resumeId", resumeId1.toString())
                        .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessions").exists())
                .andReturn();

        String responseBody3 = result3.getResponse().getContentAsString();
        ApiResponse<SessionListResponse> response3 = JSON.parseObject(
                responseBody3,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<SessionListResponse>>() {}
        );

        SessionListResponse listResponse3 = response3.getData();
        assertNotNull(listResponse3);
        assertNotNull(listResponse3.getSessions());
        // 验证所有返回的会话都满足筛选条件
        for (InterviewSession session : listResponse3.getSessions()) {
            assertEquals(resumeId1, session.getResumeId());
            assertEquals(SessionStatus.CREATED, session.getStatus());
        }
        assertTrue(listResponse3.getTotal() >= 2); // 至少包含我们创建的2个会话
    }

    /**
     * 辅助方法：创建会话并返回会话ID
     */
    private Long createSessionAndGetId(Long resumeId, Integer durationMinutes) throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setResumeId(resumeId);
        request.setDurationMinutes(durationMinutes);

        MvcResult result = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<Long> response = JSON.parseObject(
                responseBody,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {}
        );
        return response.getData();
    }

    @Test
    void testGetNextQuestion_ReturnsQuestionAndTurnId() throws Exception {
        Mockito.when(llmService.chat(anyList())).thenReturn("请描述你在项目中解决过的性能瓶颈。");

        InterviewCreateRequest createRequest = new InterviewCreateRequest();
        createRequest.setResumeId(1L);
        createRequest.setDurationMinutes(30);

        MvcResult createResult = mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        ApiResponse<Long> createResponse = JSON.parseObject(
                createResponseBody,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<Long>>() {}
        );
        Long sessionId = createResponse.getData();

        MvcResult nextQuestionResult = mockMvc.perform(post("/api/interview/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.question").exists())
                .andExpect(jsonPath("$.data.turnId").exists())
                .andReturn();

        String nextQuestionBody = nextQuestionResult.getResponse().getContentAsString();
        ApiResponse<NextQuestionResponse> nextQuestionResponse = JSON.parseObject(
                nextQuestionBody,
                new com.alibaba.fastjson2.TypeReference<ApiResponse<NextQuestionResponse>>() {}
        );

        assertNotNull(nextQuestionResponse.getData());
        assertNotNull(nextQuestionResponse.getData().getQuestion());
        assertFalse(nextQuestionResponse.getData().getQuestion().isEmpty());
        assertNotNull(nextQuestionResponse.getData().getTurnId());
        assertTrue(nextQuestionResponse.getData().getTurnId() > 0);
    }
}
