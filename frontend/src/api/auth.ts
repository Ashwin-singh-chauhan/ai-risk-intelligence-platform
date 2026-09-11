import { apiClient } from "./client";
import type { AuthResponse } from "../types/domain";

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/v1/auth/login", { email, password });
  return data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post("/v1/auth/logout", { refreshToken });
}
