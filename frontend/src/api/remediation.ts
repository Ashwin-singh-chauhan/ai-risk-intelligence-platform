import { apiClient } from "./client";
import type { PageResponse, Remediation } from "../types/domain";

export async function listRemediations(params: { status?: string; priority?: string; page?: number; size?: number }) {
  const { data } = await apiClient.get<PageResponse<Remediation>>("/v1/remediations", { params });
  return data;
}

export async function updateRemediationStatus(id: string, status: string): Promise<Remediation> {
  const { data } = await apiClient.patch<Remediation>(`/v1/remediations/${id}/status`, null, { params: { status } });
  return data;
}
