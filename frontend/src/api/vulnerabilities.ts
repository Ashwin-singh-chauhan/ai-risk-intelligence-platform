import { apiClient } from "./client";
import type { PageResponse, Vulnerability } from "../types/domain";

export interface VulnerabilitySearchParams {
  severity?: string;
  status?: string;
  assetId?: string;
  query?: string;
  page?: number;
  size?: number;
}

export async function searchVulnerabilities(params: VulnerabilitySearchParams): Promise<PageResponse<Vulnerability>> {
  const { data } = await apiClient.get<PageResponse<Vulnerability>>("/v1/vulnerabilities", { params });
  return data;
}

export async function updateVulnerabilityStatus(id: string, status: string, notes?: string): Promise<Vulnerability> {
  const { data } = await apiClient.patch<Vulnerability>(`/v1/vulnerabilities/${id}/status`, null, {
    params: { status, notes },
  });
  return data;
}
