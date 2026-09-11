import { apiClient } from "./client";
import type { PageResponse, SecurityEvent } from "../types/domain";

export async function searchEvents(params: { assetId?: string; severity?: string; anomalyOnly?: boolean; page?: number; size?: number }) {
  const { data } = await apiClient.get<PageResponse<SecurityEvent>>("/v1/events", { params });
  return data;
}

export async function listAnomalies(params: { page?: number; size?: number }) {
  const { data } = await apiClient.get<PageResponse<SecurityEvent>>("/v1/events/anomalies", { params });
  return data;
}

export async function getEventStats(hours = 24) {
  const { data } = await apiClient.get<{ totalEvents: number; anomalyEvents: number; windowHours: number }>(
    "/v1/events/stats",
    { params: { hours } }
  );
  return data;
}
