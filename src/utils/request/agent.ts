export interface AgentAskRequest {
  term?: string;
  question: string;
  context?: string;
  bookId?: string;
  bookTitle?: string;
  cfi?: string;
}

export interface AgentSource {
  sourceId: string;
  title: string;
  url: string;
  domain: string;
}

export interface AgentUsage {
  inputTokens: number;
  outputTokens: number;
  latencyMs: number;
}

export interface AgentAskResponse {
  qaId: string;
  route: string;
  term?: string;
  question?: string;

  oneLine: string;
  explanation: string;
  keyPoint: string;

  historyVsLegend?: string | null;
  evidenceLevel: string;

  sources: AgentSource[];
  usage?: AgentUsage;
}

const AGENT_BASE_URL = "http://127.0.0.1:8080";

export async function askAgent(
  request: AgentAskRequest
): Promise<AgentAskResponse> {
  const response = await fetch(
    `${AGENT_BASE_URL}/api/v1/agent/ask`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    }
  );

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
      `Agent request failed: ${response.status} ${errorText}`
    );
  }

  return (await response.json()) as AgentAskResponse;
}
export type FeedbackRating = "TOO_SHALLOW" | "JUST_RIGHT" | "TOO_DEEP";

export interface FeedbackRequest {
  qaId: string;
  rating: FeedbackRating;
  comment?: string;
}

export interface FeedbackResponse {
  success: boolean;
}

export async function sendFeedback(
  body: FeedbackRequest
): Promise<FeedbackResponse> {
  const res = await fetch(`${AGENT_BASE_URL}/api/v1/agent/feedback`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(`Feedback failed: ${res.status}`);
  }

  return (await res.json()) as FeedbackResponse;
}