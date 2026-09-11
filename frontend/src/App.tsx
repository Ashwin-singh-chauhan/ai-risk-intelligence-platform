import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { AnalystDashboardPage } from "./pages/AnalystDashboardPage";
import { ExecutiveDashboardPage } from "./pages/ExecutiveDashboardPage";
import { AssetsPage } from "./pages/AssetsPage";
import { VulnerabilitiesPage } from "./pages/VulnerabilitiesPage";
import { IncidentsPage } from "./pages/IncidentsPage";
import { RemediationPage } from "./pages/RemediationPage";
import { CompliancePage } from "./pages/CompliancePage";
import { AssistantPage } from "./pages/AssistantPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/assets" replace />} />
        <Route
          path="/analyst"
          element={
            <ProtectedRoute roles={["ADMIN", "SECURITY_ANALYST"]}>
              <AnalystDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/executive"
          element={
            <ProtectedRoute roles={["ADMIN", "EXECUTIVE"]}>
              <ExecutiveDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route path="/assets" element={<AssetsPage />} />
        <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
        <Route path="/incidents" element={<IncidentsPage />} />
        <Route
          path="/remediation"
          element={
            <ProtectedRoute roles={["ADMIN", "SECURITY_ANALYST"]}>
              <RemediationPage />
            </ProtectedRoute>
          }
        />
        <Route path="/compliance" element={<CompliancePage />} />
        <Route path="/assistant" element={<AssistantPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
