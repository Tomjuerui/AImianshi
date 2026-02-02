export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  const payload: ApiResponse<T> = await response.json();
  if (payload.code !== 0) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

export function getSession(sessionId: number) {
  return request(`/api/interview/sessions/${sessionId}`);
}

export function createSession(resumeId: number, durationMinutes: number) {
  return request<number>("/api/interview/sessions", {
    method: "POST",
    body: JSON.stringify({ resumeId, durationMinutes }),
  });
}

export function addTurn(sessionId: number, role: string, content: string) {
  return request<number>(`/api/interview/sessions/${sessionId}/turns`, {
    method: "POST",
    body: JSON.stringify({ role, content }),
  });
}

export function advanceStage(sessionId: number) {
  return request(`/api/interview/sessions/${sessionId}/stage/next`, {
    method: "POST",
  });
}

export function endSession(sessionId: number) {
  return request<number>(`/api/interview/sessions/${sessionId}/end`, {
    method: "POST",
  });
}

export function generateReport(sessionId: number) {
  return request(`/api/interview/sessions/${sessionId}/report`, {
    method: "POST",
  });
}

export function getReport(sessionId: number) {
  return request(`/api/interview/sessions/${sessionId}/report`);
}
