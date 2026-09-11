import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import type { Role } from "../types/domain";

export function ProtectedRoute({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { isAuthenticated, hasAnyRole } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (roles && !hasAnyRole(roles)) {
    return <Navigate to="/assets" replace />;
  }
  return <>{children}</>;
}
