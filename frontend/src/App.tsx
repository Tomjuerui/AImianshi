import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  addTurn,
  advanceStage,
  createSession,
  endSession,
  generateReport,
  getReport,
  getSession,
} from "./api";
import {
  InterviewReport,
  InterviewTurn,
  SessionDetail,
  StagePlanStage,
} from "./types";

const DEFAULT_RESUME_ID = 1;
const DEFAULT_DURATION = 30;

type StreamingState = {
  isStreaming: boolean;
  content: string;
};

export default function App() {
  const [resumeId, setResumeId] = useState(DEFAULT_RESUME_ID);
  const [durationMinutes, setDurationMinutes] = useState(DEFAULT_DURATION);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [sessionDetail, setSessionDetail] = useState<SessionDetail | null>(null);
  const [candidateText, setCandidateText] = useState("");
  const [report, setReport] = useState<InterviewReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [streaming, setStreaming] = useState<StreamingState>({
    isStreaming: false,
    content: "",
  });
  const eventSourceRef = useRef<EventSource | null>(null);

  const stages = useMemo<StagePlanStage[]>(() => {
    if (!sessionDetail?.session.stagePlanJson) {
      return [];
    }
    try {
      return JSON.parse(sessionDetail.session.stagePlanJson) as StagePlanStage[];
    } catch {
      return [];
    }
  }, [sessionDetail]);

  const loadSessionDetail = useCallback(async () => {
    if (!sessionId) {
      return;
    }
    const data = (await getSession(sessionId)) as SessionDetail;
    setSessionDetail(data);
  }, [sessionId]);

  const handleCreateSession = async () => {
    try {
      setError(null);
      const id = await createSession(resumeId, durationMinutes);
      setSessionId(id);
      setReport(null);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleSendAnswer = async () => {
    if (!sessionId || !candidateText.trim()) {
      return;
    }
    try {
      setError(null);
      await addTurn(sessionId, "CANDIDATE", candidateText.trim());
      setCandidateText("");
      await loadSessionDetail();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleAdvanceStage = async () => {
    if (!sessionId) {
      return;
    }
    try {
      setError(null);
      await advanceStage(sessionId);
      await loadSessionDetail();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleEndSession = async () => {
    if (!sessionId) {
      return;
    }
    try {
      setError(null);
      await endSession(sessionId);
      await loadSessionDetail();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleGenerateReport = async () => {
    if (!sessionId) {
      return;
    }
    try {
      setError(null);
      const data = (await generateReport(sessionId)) as InterviewReport;
      setReport(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleFetchReport = async () => {
    if (!sessionId) {
      return;
    }
    try {
      setError(null);
      const data = (await getReport(sessionId)) as InterviewReport;
      setReport(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleStartStreaming = () => {
    if (!sessionId || streaming.isStreaming) {
      return;
    }
    setError(null);
    setStreaming({ isStreaming: true, content: "" });
    const source = new EventSource(
      `/api/interview/sessions/${sessionId}/next-question/stream`
    );
    eventSourceRef.current = source;

    source.addEventListener("chunk", (event) => {
      const data = (event as MessageEvent<string>).data;
      setStreaming((prev) => ({
        isStreaming: true,
        content: prev.content + data,
      }));
    });

    source.addEventListener("done", async (event) => {
      const data = (event as MessageEvent<string>).data;
      try {
        const payload = JSON.parse(data) as { question: string };
        setStreaming({ isStreaming: false, content: "" });
        await loadSessionDetail();
        setError(null);
        if (!payload.question) {
          setError("未获取到问题内容");
        }
      } catch {
        setError("解析 SSE 数据失败");
      } finally {
        source.close();
      }
    });

    source.addEventListener("error", (event) => {
      setError((event as MessageEvent<string>).data || "SSE 连接失败");
      setStreaming({ isStreaming: false, content: "" });
      source.close();
    });
  };

  const handleStopStreaming = () => {
    eventSourceRef.current?.close();
    eventSourceRef.current = null;
    setStreaming({ isStreaming: false, content: "" });
  };

  useEffect(() => {
    if (sessionId) {
      loadSessionDetail();
    }
  }, [sessionId, loadSessionDetail]);

  useEffect(() => {
    return () => {
      eventSourceRef.current?.close();
    };
  }, []);

  const turns: InterviewTurn[] = sessionDetail?.turns ?? [];

  return (
    <div className="page">
      <header className="header">
        <h1>AI 模拟面试 Demo</h1>
        <p>展示阶段推进、SSE 追问与报告生成的完整流程。</p>
      </header>

      <section className="card session-card">
        <h2>创建会话</h2>
        <div className="session-form">
          <label>
            Resume ID
            <input
              type="number"
              value={resumeId}
              onChange={(event) => setResumeId(Number(event.target.value))}
            />
          </label>
          <label>
            Duration (min)
            <input
              type="number"
              value={durationMinutes}
              onChange={(event) => setDurationMinutes(Number(event.target.value))}
            />
          </label>
          <button onClick={handleCreateSession}>创建会话</button>
        </div>
        {sessionDetail && (
          <div className="session-meta">
            <div>Session ID: {sessionDetail.session.id}</div>
            <div>Status: {sessionDetail.session.status}</div>
            <div>Current Stage: {sessionDetail.session.currentStage}</div>
            <div>Started At: {sessionDetail.session.startedAt || "-"}</div>
            <div>Ended At: {sessionDetail.session.endedAt || "-"}</div>
          </div>
        )}
        {error && <p className="error">{error}</p>}
      </section>

      <div className="grid">
        <section className="card">
          <h3>阶段进度</h3>
          {stages.length === 0 && <p>暂无阶段计划。</p>}
          <ul className="stage-list">
            {stages.map((stage) => (
              <li
                key={stage.code}
                className={
                  stage.code === sessionDetail?.session.currentStage
                    ? "stage-item active"
                    : "stage-item"
                }
              >
                <strong>{stage.name}</strong>
                <span>{stage.code}</span>
                <p>{stage.goal}</p>
                <span className="tag">建议轮次 {stage.minTurns}</span>
              </li>
            ))}
          </ul>
          <button onClick={handleAdvanceStage} disabled={!sessionId}>
            推进阶段
          </button>
        </section>

        <section className="card chat-card">
          <h3>对话区</h3>
          <div className="turns">
            {turns.map((turn) => (
              <div key={turn.id} className={`turn ${turn.role.toLowerCase()}`}>
                <div className="turn-meta">
                  <span>{turn.role}</span>
                  {turn.stageCode && <span>阶段 {turn.stageCode}</span>}
                </div>
                <div className="turn-content">{turn.contentText}</div>
              </div>
            ))}
            {streaming.isStreaming && (
              <div className="turn interviewer streaming">
                <div className="turn-meta">
                  <span>INTERVIEWER</span>
                  <span>生成中...</span>
                </div>
                <div className="turn-content">{streaming.content}</div>
              </div>
            )}
          </div>
          <div className="input-row">
            <input
              value={candidateText}
              onChange={(event) => setCandidateText(event.target.value)}
              placeholder="输入候选人回答"
            />
            <button onClick={handleSendAnswer} disabled={!sessionId}>
              发送回答
            </button>
          </div>
          <div className="actions">
            <button onClick={handleStartStreaming} disabled={!sessionId}>
              下一问（SSE）
            </button>
            <button onClick={handleStopStreaming} disabled={!streaming.isStreaming}>
              停止
            </button>
            <button onClick={handleEndSession} disabled={!sessionId}>
              结束会话
            </button>
          </div>
        </section>

        <section className="card report-card">
          <h3>报告区</h3>
          <div className="actions">
            <button onClick={handleGenerateReport} disabled={!sessionId}>
              生成报告
            </button>
            <button onClick={handleFetchReport} disabled={!sessionId}>
              查询报告
            </button>
          </div>
          {!report && <p>暂无报告。</p>}
          {report && (
            <div className="report">
              <div className="score">综合评分：{report.overallScore}</div>
              <p className="summary">{report.summary}</p>
              <div className="report-section">
                <h4>优势</h4>
                <p>{report.strengths}</p>
              </div>
              <div className="report-section">
                <h4>不足</h4>
                <p>{report.weaknesses}</p>
              </div>
              <div className="report-section">
                <h4>建议</h4>
                <p>{report.suggestions}</p>
              </div>
              {report.stageReports && report.stageReports.length > 0 && (
                <div className="report-section">
                  <h4>阶段小结</h4>
                  <ul className="stage-report-list">
                    {report.stageReports.map((item) => (
                      <li key={item.id}>
                        <strong>{item.stageCode}</strong>
                        <p>{item.summary}</p>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
