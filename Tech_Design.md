# 技术设计文档 TDD（Java Spring + 千问/DeepSeek）

## 0. 概述

### 0.1 目标

- 用户上传简历 → 系统抽取项目/技术点 → AI 语音面试（追问/打断/压强） → 生成结构化复盘报告
- MVP 优先：**可用 > 完美**，避免过度工程

### 0.2 核心原则

- **面试编排（状态机）**与**模型调用（网关）**解耦：以后换模型/加策略不动主流程
- 输出强结构化：复盘阶段必须 JSON Schema，避免前端解析地狱
- 成本/延迟可控：对话用“快模型”，报告用“慢模型”（同一家也行，配置层实现）

------

## 1. 技术选型

### 1.1 后端（Java）

- JDK 17
- Spring Boot 3.x
- Spring Web (REST) / WebSocket（语音实时可选）
- Spring Security（简单 JWT）
- Spring Data JPA 或 MyBatis-Plus（二选一，个人项目推荐 MyBatis-Plus 快）
- Redis（会话状态、限流、缓存）
- MySQL 8（持久化）
- MinIO / S3（简历文件、报告导出）
- 消息/异步：Spring @Async 或 RabbitMQ（MVP 用 @Async + 线程池即可）

### 1.2 AI 模型接入

- **模型网关层**抽象：`LLMClient` 接口 + 适配器
- Provider：
  - Qwen（千问）
  - DeepSeek
- 关键能力：支持 **流式输出**（SSE）更像真人

### 1.3 语音（ASR/TTS）

为了“简单可落地”，推荐 **语音在前端做**：

- 前端用浏览器 Web Speech API（若可用）做 TTS；ASR 用浏览器/第三方 SDK
- 后端只收文本（MVP）
   后续若你要全后端化：
- ASR/TTS 作为独立服务 `speech-service`，接口标准化（见第 6 节）

------

## 2. 总体架构

### 2.1 逻辑架构（分层）

- **API 层**：认证、面试、简历、报告
- **Domain 层**：面试会话、阶段、题目、追问策略、评分点
- **Service 层**：
  - ResumeService：简历解析与深挖点生成
  - InterviewOrchestrator：面试编排/状态机推进
  - LLMGateway：模型路由、重试、流式
  - ReportService：复盘 JSON 生成 + 渲染导出
- **Infra 层**：
  - MySQL/Redis/ObjectStore
  - Provider SDK（Qwen/DeepSeek）
  - 日志审计、脱敏

### 2.2 组件图（建议拆 2~3 个服务也可单体）

**单体（最简单）**：一个 Spring Boot 应用包含所有模块
 **稍微规范**：按 package 分层，保留未来拆分空间

- `interview-service`（主）
- `llm-gateway`（可先内置在主服务，未来再拆）

------

## 3. 核心数据模型（DB 设计）

> 目标：够用、能复盘、能做对话回放。

### 3.1 表结构

#### user（可选，个人用也建议保留）

- id (PK)
- email / username
- password_hash（也可只做匿名 token）
- created_at

#### resume

- id (PK)
- user_id
- file_url（对象存储地址）
- raw_text（解析出的全文，建议可选存）
- extracted_json（结构化抽取：项目/技能/指标）
- created_at

#### interview_session

- id (PK)
- user_id
- resume_id
- duration_minutes (30/45/60)
- status (CREATED/RUNNING/FINISHED/FAILED)
- current_stage（枚举）
- stage_plan_json（本次权重/计划）
- started_at / ended_at
- created_at

#### interview_turn（对话回放）

- id (PK)
- session_id
- role (INTERVIEWER/CANDIDATE/SYSTEM)
- content_text
- audio_url（可选：若你保存语音）
- token_usage（可选）
- created_at

#### interview_signal（评分线索，边聊边记）

- id (PK)
- session_id
- dimension（KNOWLEDGE/DEPTH/STRUCTURE/ENGINEERING/COMM）
- score_delta（可正可负）
- reason（短句）
- evidence_turn_id（对应对话）
- created_at

#### report

- id (PK)
- session_id (unique)
- total_score
- grade (S/A/B/C)
- dimension_scores_json
- strengths_json
- issues_json
- knowledge_gaps_json
- next_actions_json
- report_json（完整结构化报告）
- pdf_url / md_url（可选）
- created_at

------

## 4. 面试编排（状态机）设计

### 4.1 阶段枚举

```
INTRO -> JAVA_BASIC -> SPRING -> DB_CACHE -> MIDDLEWARE(Optional) -> SYSTEM_DESIGN -> END
```

### 4.2 状态机推进规则

每轮问答后，`InterviewOrchestrator` 做：

1. 将候选人回答写入 `interview_turn`
2. 调用 **追问判定器**（规则 + LLM）：
   - 如果回答“空泛/不完整/矛盾/有数字无证据” → 进入 `FOLLOW_UP` 子状态（但不改变主 stage）
   - 否则按 stage 的目标问题数 / 时间预算推进下一题
3. 记录评分线索 `interview_signal`
4. 若时间到 → 强制进入下一 stage 或 END

### 4.3 追问策略（MVP 可规则优先）

建议先用 **规则** 做 70% 的拷打效果，再用 LLM 精细化：

- 命中词：
  - “优化了/提升了/解决了” → 追问：优化前后指标、瓶颈定位方法、验证方式
  - “高并发/高可用” → 追问：限流、熔断、降级、监控告警、压测
  - “用到了 Redis/MQ/ES” → 追问：为什么选它、数据一致性、幂等、重试策略
- 空泛检测（简单正则/长度/含糊词表）：
  - “大概/差不多/应该/可能/一般/比较” → 打断要求具体
- 矛盾检测（MVP 用 LLM 判断即可）

------

## 5. 模型网关（Qwen / DeepSeek）设计

### 5.1 抽象接口

```
public interface LLMClient {
  Stream<String> chatStream(ChatRequest req); // SSE/Flux<String>
  ChatResponse chat(ChatRequest req);         // 非流式
}
```

### 5.2 Provider 适配器

- `QwenClient implements LLMClient`
- `DeepSeekClient implements LLMClient`

### 5.3 路由策略（配置驱动）

- `llm.provider=QWEN|DEEPSEEK`
- `llm.mode.fast=model_x`
- `llm.mode.slow=model_y`
- 对话用 fast，报告用 slow

### 5.4 重试与降级

- 失败重试：指数退避 2~3 次
- 超时：对话 15s / 报告 60s（可配）
- 降级：切换另一个 provider 或返回“离线题库模式”（MVP 可以先只提示失败）

### 5.5 Prompt 结构（强建议分层）

- System Prompt（面试官人格、禁令、压强规则）
- Context（简历抽取 JSON、当前阶段目标、剩余时间）
- History（最近 N 轮对话，N=6~12，防 token 爆）
- Output Constraint（复盘阶段强 JSON schema）

------

## 6. 语音链路（两种实现）

### 6.1 MVP（推荐：前端语音，后端文本）

- 前端 ASR → 文本发后端
- 后端流式返回面试官文本 → 前端 TTS 播放
- 优点：最省事、成本最低

### 6.2 后端语音（可选扩展）

新增 `SpeechService`：

- `POST /speech/asr`：音频 → 文本
- `POST /speech/tts`：文本 → 音频
   并在 `interview_turn` 存 `audio_url`

------

## 7. API 设计（REST + SSE）

### 7.1 简历

- `POST /api/resumes/upload`
  - multipart file
  - 返回：resumeId
- `GET /api/resumes/{id}`
  - 返回：抽取结果（项目、技术点、可疑点）

### 7.2 面试会话

- `POST /api/interviews`
  - body: `{ resumeId, durationMinutes }`
  - 返回：`sessionId`
- `POST /api/interviews/{sessionId}/start`
  - 开始面试，返回第一问（或提供 stream）

### 7.3 对话（文本）

- `POST /api/interviews/{sessionId}/turn`
  - body: `{ candidateText }`
  - 返回：`{ interviewerText, stage, isFollowUp }`

### 7.4 对话（流式，强推荐）

- `GET /api/interviews/{sessionId}/stream?candidateText=...`
  - 返回 SSE 流：逐 token/逐句输出面试官话术

### 7.5 结束与报告

- `POST /api/interviews/{sessionId}/finish`
  - 触发异步生成报告
- `GET /api/reports/{sessionId}`
  - 返回 report_json（结构化）
- `GET /api/reports/{sessionId}/export?format=pdf|md`
  - 返回下载链接（对象存储 URL）

------

## 8. 报告生成（异步任务）

### 8.1 输入

- session 基础信息（时长、阶段走到哪）
- 最近全量对话 turns
- scoring signals（若有）

### 8.2 输出（强 JSON）

建议定义 JSON Schema（简化示例）：

```
{
  "totalScore": 78,
  "grade": "B",
  "dimensionScores": { "knowledge": 75, "depth": 70, "structure": 82, "engineering": 76, "communication": 85 },
  "strengths": [{ "title": "...", "evidenceTurnId": 123 }],
  "issues": [{ "title": "...", "why": "...", "fix": "...", "evidenceTurnId": 145 }],
  "knowledgeGaps": [{ "tag": "JVM", "items": ["...","..."] }],
  "nextActions": [{ "action": "复盘项目监控口径", "priority": "HIGH" }]
}
```

### 8.3 渲染导出

- MVP：前端渲染即可
- 需要导出 PDF：后端用模板（Freemarker/Thymeleaf）+ OpenPDF / iText（注意授权）
   或者直接导出 Markdown 最省事

------

## 9. 安全与隐私（个人项目也要做底线）

- 简历与对话落库前可做脱敏（手机号/身份证/邮箱）
- 对象存储 URL 私有读（签名 URL）
- 日志禁止打印完整简历原文（只打印 hash/摘要）
- 提供删除接口：
  - `DELETE /api/users/me/data`：删除简历、会话、报告、对象存储文件

------

## 10. 可观测性与运维

- 日志：logback + traceId（每 session 一个）
- 指标：模型调用耗时、失败率、token 用量（可选）
- 限流：按 user/session 限制 QPS（Redis 令牌桶）

------

## 11. 目录结构建议（单体 Spring Boot）

```
com.xxx.interview
  ├── api (controller)
  ├── domain
  │    ├── interview (Session, Stage, Orchestrator)
  │    ├── resume
  │    └── report
  ├── service
  ├── infra
  │    ├── llm (LLMClient, QwenClient, DeepSeekClient, Router)
  │    ├── storage (S3/Minio)
  │    ├── repo (mapper/jpa)
  │    └── cache (redis)
  └── common (exception, dto, utils, security)
```

------

## 12. MVP 交付清单（按最短路径）

### M1（能跑通）

- 简历上传 + 文本抽取（先用 pdfbox 提取全文）
- 创建 session + 固定阶段流程
- 对话接口（非流式也行）
- 报告生成（异步，JSON 输出）

### M2（更像真人拷打）

- SSE 流式输出
- 追问规则完善（空泛/数字/优化/一致性）
- 语音（前端 ASR/TTS）

------

## 13. 风险点与应对

- **简历解析质量差**：MVP 只抽“技术关键词 + 项目段落”，深挖点可由 LLM 二次总结
- **模型输出不稳定**：复盘阶段强制 schema + 校验，不合格则重试一次
- **token 过长**：只保留最近 N 轮 + 简历抽取 JSON，不塞全文
- **延迟**：对话用 fast 模型；输出用 SSE；报告异步