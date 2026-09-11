import { apiClient } from "./client";
import type { RiskScore } from "../types/domain";

export async function getLatestRiskForAllAssets(): Promise<RiskScore[]> {
  const { data } = await apiClient.get<RiskScore[]>("/v1/risk-scores/latest");
  return data;
}

export async function getLatestRiskForAsset(assetId: string): Promise<RiskScore> {
  const { data } = await apiClient.get<RiskScore>(`/v1/risk-scores/assets/${assetId}/latest`);
  return data;
}

export async function recomputeRisk(assetId: string): Promise<RiskScore> {
  const { data } = await apiClient.post<RiskScore>(`/v1/risk-scores/assets/${assetId}/recompute`);
  return data;
}
