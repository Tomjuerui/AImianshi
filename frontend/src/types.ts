export interface StagePlanStage {
  code: string;
  name: string;
  goal: string;
  minTurns: number;
}

export interface InterviewSession {
  id: number;
  status: string;
  currentStage: string;
  stagePlanJson: string;
  startedAt?: string;
  endedAt?: string;
}

export interface InterviewTurn {
  id: number;
  role: string;
  contentText: string;
  stageCode?: string;
  createdAt: string;
}

export interface SessionDetail {
  session: InterviewSession;
  turns: InterviewTurn[];
}

export interface StageMiniReport {
  id: number;
  stageCode: string;
  score: number;
  summary: string;
  strengths: string;
  weaknesses: string;
  suggestions: string;
}

export interface InterviewReport {
  id: number;
  overallScore: number;
  summary: string;
  strengths: string;
  weaknesses: string;
  suggestions: string;
  stageReports?: StageMiniReport[];
  createdAt: string;
  updatedAt: string;
}
