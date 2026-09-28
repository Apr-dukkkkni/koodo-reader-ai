export interface AgentAskRequest {
  question: string;
  context?: string;
  bookId?: string;
  bookTitle?: string;
}

export interface AgentAskResponse {
  qaId: string;
  route: string;
  question: string;
  oneLine: string;
  explanation: string;
  keyPoint: string;
  evidenceLevel: string;
  sources: any[];
}

export async function askAgent(
  request: AgentAskRequest
): Promise<AgentAskResponse> {
  const response = await fetch(
    "http://127.0.0.1:8080/api/v1/agent/ask",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    }
  );

  if (!response.ok) {
    throw new Error(`Agent request failed: ${response.status}`);
  }

  return await response.json();
}