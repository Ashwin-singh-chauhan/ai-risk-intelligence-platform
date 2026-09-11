import { apiClient } from "./client";
import type { AskResponse } from "../types/domain";

export async function askAssistant(question: string): Promise<AskResponse> {
  const { data } = await apiClient.post<AskResponse>("/v1/rag/ask", { question });
  return data;
}
