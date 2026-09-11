import { apiClient } from "./client";
import type { Asset, PageResponse } from "../types/domain";

export interface AssetSearchParams {
  businessUnit?: string;
  criticality?: string;
  assetType?: string;
  query?: string;
  page?: number;
  size?: number;
}

export async function searchAssets(params: AssetSearchParams): Promise<PageResponse<Asset>> {
  const { data } = await apiClient.get<PageResponse<Asset>>("/v1/assets", { params });
  return data;
}

export async function getAsset(id: string): Promise<Asset> {
  const { data } = await apiClient.get<Asset>(`/v1/assets/${id}`);
  return data;
}

export async function listBusinessUnits(): Promise<string[]> {
  const { data } = await apiClient.get<string[]>("/v1/assets/business-units");
  return data;
}
