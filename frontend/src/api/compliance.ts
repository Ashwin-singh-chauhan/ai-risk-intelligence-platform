import { apiClient } from "./client";
import type { ComplianceControl, ComplianceFramework } from "../types/domain";

export async function listComplianceControls(framework: ComplianceFramework, businessUnit?: string) {
  const { data } = await apiClient.get<ComplianceControl[]>("/v1/compliance/controls", {
    params: { framework, businessUnit },
  });
  return data;
}

export async function getCompliancePosture(framework: ComplianceFramework, businessUnit?: string) {
  const { data } = await apiClient.get("/v1/compliance/posture", { params: { framework, businessUnit } });
  return data as { framework: string; totalControls: number; compliant: number; partial: number; nonCompliant: number; notAssessed: number; compliancePercentage: number };
}

export async function submitAssessment(controlId: string, businessUnit: string, status: string, evidenceSummary?: string) {
  await apiClient.post("/v1/compliance/assessments", { controlId, businessUnit, status, evidenceSummary });
}
