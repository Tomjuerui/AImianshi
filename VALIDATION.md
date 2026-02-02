# Interview Session 模块验收文档

## 测试结果

✅ **所有测试通过**: 17 个测试用例，0 失败，0 错误

## 本地验证步骤

### 前置条件

1. 启动应用（使用 dev 环境，H2 内存数据库）：
```bash
cd aimian
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或者使用 IDE 运行 `AimianApplication`，激活 `dev` profile。

应用默认运行在 `http://localhost:8080`

### 1. 创建面试会话

```bash
curl -X POST http://localhost:8080/api/interview/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "resumeId": 1,
    "durationMinutes": 30
  }'
```

**预期响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": 1
}
```

保存返回的 `data` 值作为 `sessionId`（例如：1）

### 2. 追加对话轮次（面试官提问）

```bash
curl -X POST http://localhost:8080/api/interview/sessions/1/turns \
  -H "Content-Type: application/json" \
  -d '{
    "content": "What is your experience with Java?",
    "role": "INTERVIEWER"
  }'
```

**预期响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": 1
}
```

### 3. 追加对话轮次（候选人回答）

```bash
curl -X POST http://localhost:8080/api/interview/sessions/1/turns \
  -H "Content-Type: application/json" \
  -d '{
    "content": "I have 3 years of Java development experience.",
    "role": "CANDIDATE"
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": 2
}
```

### 4. 查询会话详情（包含所有 turns）

```bash
curl -X GET http://localhost:8080/api/interview/sessions/1
```

**预期响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "session": {
      "id": 1,
      "userId": 1,
      "resumeId": 1,
      "durationMinutes": 30,
      "status": "CREATED",
      "currentStage": null,
      "stagePlanJson": null,
      "startedAt": null,
      "endedAt": null,
      "createdAt": "2026-02-01T21:14:03.670287",
      "updatedAt": "2026-02-01T21:14:03.671806"
    },
    "turns": [
      {
        "id": 1,
        "sessionId": 1,
        "role": "INTERVIEWER",
        "contentText": "What is your experience with Java?",
        "audioUrl": null,
        "tokenUsage": null,
        "createdAt": "2026-02-01T21:14:03.680603"
      },
      {
        "id": 2,
        "sessionId": 1,
        "role": "CANDIDATE",
        "contentText": "I have 3 years of Java development experience.",
        "audioUrl": null,
        "tokenUsage": null,
        "createdAt": "2026-02-01T21:14:03.690133"
      }
    ]
  }
}
```

### 5. 获取下一道面试问题

```bash
curl -X POST http://localhost:8080/api/interview/sessions/1/next-question
```

**预期响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "question": "请描述你在项目中解决过的性能瓶颈。",
    "turnId": 3
  }
}
```

### 6. 获取下一道面试问题（SSE 流式）

```bash
curl -N http://localhost:8080/api/interview/sessions/1/next-question/stream
```

**预期输出**:
```
event: chunk
data: 请分享

event: chunk
data: 一次你

event: done
data: {"turnId":3,"question":"请分享一次你解决线上故障的经历。"}
```

### 7. 会话状态机流转（CREATED -> RUNNING -> ENDED）

```bash
# 1) 创建会话后，status=CREATED，startedAt/endedAt 为 null
curl -X POST http://localhost:8080/api/interview/sessions \
  -H "Content-Type: application/json" \
  -d '{"resumeId": 1, "durationMinutes": 30}'

curl -X GET http://localhost:8080/api/interview/sessions/1 | jq '.data.session'

# 2) 首次调用 next-question 后，status=RUNNING，startedAt 有值
curl -X POST http://localhost:8080/api/interview/sessions/1/next-question | jq
curl -X GET http://localhost:8080/api/interview/sessions/1 | jq '.data.session'

# 3) 调用 end 后，status=ENDED，endedAt 有值
curl -X POST http://localhost:8080/api/interview/sessions/1/end | jq
curl -X GET http://localhost:8080/api/interview/sessions/1 | jq '.data.session'
```

### 8. 生成与查询面试报告

```bash
# 前置：会话已结束
curl -X POST http://localhost:8080/api/interview/sessions/1/report | jq

curl -X GET http://localhost:8080/api/interview/sessions/1/report | jq
```

**预期响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "sessionId": 1,
    "overallScore": 78,
    "summary": "候选人共回答2次，平均回答长度约36字，综合评分为78分。",
    "strengths": "[\"回答内容较为充分，信息量充足\"]",
    "weaknesses": "[\"部分回答缺少结构化表达\"]",
    "suggestions": "[\"保持回答的清晰结构，并突出关键技术点\",\"结合具体项目经验举例，增强说服力\"]",
    "createdAt": "2026-02-01T21:14:03.670287",
    "updatedAt": "2026-02-01T21:20:03.670287"
  }
}
```

## 参数校验测试

### 测试 1: 创建会话时缺少必填字段

```bash
curl -X POST http://localhost:8080/api/interview/sessions \
  -H "Content-Type: application/json" \
  -d '{}'
```

**预期响应** (400 Bad Request):
```json
{
  "code": 400,
  "message": "简历ID不能为空, 时长不能为空"
}
```

### 测试 2: 追加 turn 时缺少必填字段

```bash
curl -X POST http://localhost:8080/api/interview/sessions/1/turns \
  -H "Content-Type: application/json" \
  -d '{}'
```

**预期响应** (400 Bad Request):
```json
{
  "code": 400,
  "message": "内容不能为空, 角色不能为空"
}
```

### 测试 3: 无效的角色

```bash
curl -X POST http://localhost:8080/api/interview/sessions/1/turns \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Test content",
    "role": "INVALID_ROLE"
  }'
```

**预期响应** (400 Bad Request):
```json
{
  "code": 400,
  "message": "无效的角色: INVALID_ROLE"
}
```

### 测试 4: 查询不存在的会话

```bash
curl -X GET http://localhost:8080/api/interview/sessions/99999
```

**预期响应** (400 Bad Request):
```json
{
  "code": 400,
  "message": "会话不存在: 99999"
}
```

## 完整链路验证脚本

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== 1. 创建面试会话 ==="
SESSION_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/interview/sessions \
  -H "Content-Type: application/json" \
  -d '{"resumeId": 1, "durationMinutes": 30}')

SESSION_ID=$(echo $SESSION_RESPONSE | grep -o '"data":[0-9]*' | grep -o '[0-9]*')
echo "创建会话成功，sessionId: $SESSION_ID"
echo ""

echo "=== 2. 追加面试官提问 ==="
curl -s -X POST ${BASE_URL}/api/interview/sessions/${SESSION_ID}/turns \
  -H "Content-Type: application/json" \
  -d '{"content": "What is your experience with Java?", "role": "INTERVIEWER"}' | jq
echo ""

echo "=== 3. 追加候选人回答 ==="
curl -s -X POST ${BASE_URL}/api/interview/sessions/${SESSION_ID}/turns \
  -H "Content-Type: application/json" \
  -d '{"content": "I have 3 years of Java development experience.", "role": "CANDIDATE"}' | jq
echo ""

echo "=== 4. 查询会话详情 ==="
curl -s -X GET ${BASE_URL}/api/interview/sessions/${SESSION_ID} | jq
echo ""

echo "=== 5. 获取下一道面试问题 ==="
curl -s -X POST ${BASE_URL}/api/interview/sessions/${SESSION_ID}/next-question | jq
echo ""

echo "=== 6. 获取下一道面试问题（SSE） ==="
curl -N ${BASE_URL}/api/interview/sessions/${SESSION_ID}/next-question/stream
echo ""

echo "=== 7. 结束会话 ==="
curl -s -X POST ${BASE_URL}/api/interview/sessions/${SESSION_ID}/end | jq
echo ""

echo "=== 8. 生成并查询面试报告 ==="
curl -s -X POST ${BASE_URL}/api/interview/sessions/${SESSION_ID}/report | jq
curl -s -X GET ${BASE_URL}/api/interview/sessions/${SESSION_ID}/report | jq
```

## 运行测试

```bash
cd aimian
mvn test -Dtest=InterviewControllerTest
```

**预期输出**: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`

## 修改点说明

### 已修复的问题

1. **字符编码问题**: 将测试中的中文字符串改为英文，避免 JSON 序列化/反序列化时的编码问题
2. **参数校验**: 添加了完整的参数校验，包括 `@NotNull`、`@NotBlank`、`@Min` 等注解
3. **异常处理**: 在 `GlobalExceptionHandler` 中添加了参数校验异常处理，确保返回 400 状态码
4. **依赖添加**: 在 `pom.xml` 中添加了 `spring-boot-starter-validation` 依赖

### 实现的功能

1. ✅ **创建会话**: `POST /api/interview/sessions`
2. ✅ **追加 turn**: `POST /api/interview/sessions/{id}/turns`
3. ✅ **查询详情**: `GET /api/interview/sessions/{id}`
4. ✅ **参数校验**: 所有接口都有完整的参数校验
5. ✅ **错误处理**: 统一的错误响应格式
6. ✅ **集成测试**: 17 个测试用例全部通过
7. ✅ **自动追问**: `POST /api/interview/sessions/{id}/next-question`
8. ✅ **自动追问（SSE）**: `GET /api/interview/sessions/{id}/next-question/stream`
9. ✅ **结束会话**: `POST /api/interview/sessions/{id}/end`
10. ✅ **生成/查询报告**: `POST /api/interview/sessions/{id}/report` / `GET /api/interview/sessions/{id}/report`
