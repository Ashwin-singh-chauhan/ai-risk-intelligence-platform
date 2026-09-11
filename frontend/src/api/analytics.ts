import { apiClient } from "./client";
import type { AnalystDashboard, ExecutiveDashboard } from "../types/domain";

export async function getAnalystDashboard(): Promise<AnalystDashboard> {
  const { data } = await apiClient.get<AnalystDashboard>("/v1/analytics/analyst-dashboard");
  return data;
}

export async function getExecutiveDashboard(): Promise<ExecutiveDashboard> {
  const { data } = await apiClient.get<ExecutiveDashboard>("/v1/analytics/executive-dashboard");
  return data;
}
