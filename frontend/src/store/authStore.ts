import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResponse, AuthUser, Role } from "../types/domain";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setSession: (auth: AuthResponse) => void;
  logout: () => void;
  hasAnyRole: (roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      setSession: (auth) =>
        set({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          user: auth.user,
          isAuthenticated: true,
        }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false }),
      hasAnyRole: (roles) => {
        const user = get().user;
        return !!user && roles.includes(user.role);
      },
    }),
    { name: "erip-auth" }
  )
);
