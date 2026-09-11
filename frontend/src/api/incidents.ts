import { apiClient } from "./client";
import type { Incident, PageResponse } from "../types/domain";

export interface IncidentSearchParams {
  status?: string;
  severity?: string;
  assetId?: string;
  page?: number;
  size?: number;
}

export async function searchIncidents(params: IncidentSearchParams): Promise<PageResponse<Incident>> {
  const { data } = await apiClient.get<PageResponse<Incident>>("/v1/incidents", { params });
  return data;
}

export async function updateIncidentStatus(id: string, status: string, note?: string): Promise<Incident> {
  const { data } = await apiClient.patch<Incident>(`/v1/incidents/${id}/status`, null, { params: { status, note } });
  return data;
}
