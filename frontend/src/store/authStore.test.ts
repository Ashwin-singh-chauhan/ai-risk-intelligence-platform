import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "./authStore";
import type { AuthResponse } from "../types/domain";

const sampleAuth: AuthResponse = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  tokenType: "Bearer",
  expiresInSeconds: 1800,
  user: {
    id: "user-1",
    email: "analyst@erip.com",
    fullName: "Noah Analyst",
    role: "SECURITY_ANALYST",
    businessUnit: "Security Operations",
  },
};

describe("authStore", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it("starts unauthenticated", () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
  });

  it("setSession stores tokens and marks the user authenticated", () => {
    useAuthStore.getState().setSession(sampleAuth);

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.accessToken).toBe("access-token");
    expect(state.user?.email).toBe("analyst@erip.com");
  });

  it("hasAnyRole matches the current user's role", () => {
    useAuthStore.getState().setSession(sampleAuth);

    expect(useAuthStore.getState().hasAnyRole(["ADMIN", "SECURITY_ANALYST"])).toBe(true);
    expect(useAuthStore.getState().hasAnyRole(["EXECUTIVE", "VIEWER"])).toBe(false);
  });

  it("logout clears the session", () => {
    useAuthStore.getState().setSession(sampleAuth);
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.accessToken).toBeNull();
    expect(state.user).toBeNull();
  });
});
