import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";
import { useAuthStore } from "../store/authStore";
import * as authApi from "../api/auth";

vi.mock("../api/auth");

describe("LoginPage", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    vi.clearAllMocks();
  });

  it("logs in successfully and stores the session", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "token",
      refreshToken: "refresh",
      tokenType: "Bearer",
      expiresInSeconds: 1800,
      user: { id: "1", email: "analyst@erip.com", fullName: "Noah Analyst", role: "SECURITY_ANALYST", businessUnit: null },
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await userEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
    });
  });

  it("shows an error message when login fails", async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      response: { data: { message: "Invalid email or password" } },
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await userEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByText("Invalid email or password")).toBeInTheDocument();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });

  it("clicking a demo account chip fills in the email field", async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await userEvent.click(screen.getByRole("button", { name: "Executive" }));
    expect(screen.getByDisplayValue("exec@erip.com")).toBeInTheDocument();
  });
});
