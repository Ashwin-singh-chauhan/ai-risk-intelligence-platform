export type Role = "ADMIN" | "SECURITY_ANALYST" | "EXECUTIVE" | "VIEWER";

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  businessUnit: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: AuthUser;
}

export type Criticality = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type Exposure = "INTERNAL" | "DMZ" | "PUBLIC";
export type AssetType = "SERVER" | "DATABASE" | "APPLICATION" | "NETWORK_DEVICE" | "CLOUD_RESOURCE" | "ENDPOINT" | "CONTAINER";
export type AssetEnvironment = "PRODUCTION" | "STAGING" | "DEVELOPMENT" | "TEST";

export interface Asset {
  id: string;
  assetTag: string;
  name: string;
  assetType: AssetType;
  businessUnit: string;
  owner: string | null;
  criticality: Criticality;
  exposure: Exposure;
  environment: AssetEnvironment;
  ipAddress: string | null;
  hostname: string | null;
  tags: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type VulnerabilitySeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type VulnerabilityStatus = "OPEN" | "IN_PROGRESS" | "MITIGATED" | "RESOLVED" | "ACCEPTED_RISK";

export interface Vulnerability {
  id: string;
  assetId: string;
  assetName: string;
  cveId: string | null;
  title: string;
  description: string | null;
  cvssScore: number;
  severity: VulnerabilitySeverity;
  status: VulnerabilityStatus;
  exploitAvailable: boolean;
  patchAvailable: boolean;
  discoveredAt: string;
  dueDate: string | null;
  resolvedAt: string | null;
  remediationNotes: string | null;
}

export type IncidentSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type IncidentStatus = "OPEN" | "INVESTIGATING" | "CONTAINED" | "RESOLVED" | "CLOSED";

export interface Incident {
  id: string;
  incidentNumber: string;
  title: string;
  description: string | null;
  severity: IncidentSeverity;
  status: IncidentStatus;
  relatedAssetId: string | null;
  relatedAssetName: string | null;
  relatedVulnerabilityId: string | null;
  assignedAnalystName: string | null;
  rootCause: string | null;
  resolutionSummary: string | null;
  openedAt: string;
  containedAt: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
}

export type RiskTier = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface RiskScore {
  id: string;
  assetId: string;
  assetName: string;
  overallScore: number;
  riskTier: RiskTier;
  vulnerabilityComponent: number;
  exposureComponent: number;
  threatActivityComponent: number;
  complianceComponent: number;
  incidentHistoryComponent: number;
  explanation: {
    weights: Record<string, number>;
    inputs: Record<string, unknown>;
    topContributingVulnerabilities: Array<{
      id: string;
      cveId: string | null;
      title: string;
      cvssScore: number;
      exploitAvailable: boolean;
    }>;
    narrative: string;
  };
  calculatedAt: string;
}

export type RemediationPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type RemediationStatus = "PENDING" | "ACKNOWLEDGED" | "IN_PROGRESS" | "COMPLETED" | "DISMISSED";

export interface Remediation {
  id: string;
  vulnerabilityId: string;
  vulnerabilityTitle: string;
  assetId: string;
  assetName: string;
  priority: RemediationPriority;
  priorityScore: number;
  recommendation: string;
  estimatedEffortHours: number;
  status: RemediationStatus;
  generatedAt: string;
}

export type EventSeverity = "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface SecurityEvent {
  id: string;
  assetId: string | null;
  assetName: string | null;
  eventType: string;
  severity: EventSeverity;
  sourceIp: string | null;
  destinationIp: string | null;
  protocol: string | null;
  description: string | null;
  eventTime: string;
  anomalyScore: number | null;
  anomaly: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AnalystDashboard {
  totalAssets: number;
  assetsByCriticality: Record<string, number>;
  openVulnerabilities: number;
  openVulnerabilitiesBySeverity: Record<string, number>;
  openIncidents: number;
  anomalyEventsLast24h: number;
  totalEventsLast24h: number;
  topRiskAssets: Array<{ assetId: string; assetName: string; overallScore: number; riskTier: string }>;
  topRemediations: Array<{ id: string; vulnerabilityTitle: string; priority: string; priorityScore: number }>;
}

export interface ExecutiveDashboard {
  enterpriseAverageRiskScore: number;
  assetCountByRiskTier: Record<string, number>;
  riskByBusinessUnit: Array<{ businessUnit: string; averageRiskScore: number; assetCount: number }>;
  compliancePosture: Array<{ framework: string; compliancePercentage: number }>;
  openIncidents: number;
  criticalOpenVulnerabilities: number;
}

export interface AskResponse {
  answer: string;
  llmProvider: string;
  referencedDocuments: Array<{ sourceType: string; sourceId: string; title: string; similarity: number }>;
}

export type ComplianceFramework = "NIST_CSF" | "ISO_27001" | "SOC2";
export type ComplianceStatus = "COMPLIANT" | "PARTIAL" | "NON_COMPLIANT" | "NOT_ASSESSED";

export interface ComplianceControl {
  controlId: string;
  framework: ComplianceFramework;
  controlIdentifier: string;
  controlName: string;
  controlDescription: string | null;
  category: string | null;
  latestStatus: ComplianceStatus;
  lastAssessedAt: string | null;
}
