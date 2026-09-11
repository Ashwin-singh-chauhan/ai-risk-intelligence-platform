import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getAnalystDashboard } from "../api/analytics";
import { useFetch } from "../hooks/useFetch";
import { StatCard } from "../components/StatCard";
import { Card } from "../components/Card";
import { Badge } from "../components/Badge";
import { Spinner, ErrorBanner } from "../components/StatusViews";

const CRITICALITY_COLORS: Record<string, string> = {
  LOW: "#16a34a",
  MEDIUM: "#eab308",
  HIGH: "#ea580c",
  CRITICAL: "#dc2626",
};

export function AnalystDashboardPage() {
  const { data, loading, error } = useFetch(getAnalystDashboard);

  if (loading) return <Spinner />;
  if (error || !data) return <ErrorBanner message={error ?? "Unable to load dashboard"} />;

  const criticalityData = Object.entries(data.assetsByCriticality).map(([name, value]) => ({ name, value }));
  const severityData = Object.entries(data.openVulnerabilitiesBySeverity).map(([name, value]) => ({ name, value }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">Security Analyst Dashboard</h1>
        <p className="text-sm text-slate-500">Live operational view of assets, vulnerabilities, incidents, and ML-flagged anomalies.</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <StatCard label="Total Assets" value={data.totalAssets} />
        <StatCard label="Open Vulnerabilities" value={data.openVulnerabilities} accent="warning" />
        <StatCard label="Open Incidents" value={data.openIncidents} accent="danger" />
        <StatCard label="Anomalies (24h)" value={data.anomalyEventsLast24h} accent="danger" />
        <StatCard label="Events (24h)" value={data.totalEventsLast24h} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Assets by Business Criticality">
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={criticalityData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                {criticalityData.map((entry) => (
                  <Cell key={entry.name} fill={CRITICALITY_COLORS[entry.name] ?? "#64748b"} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 8 }} />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Open Vulnerabilities by Severity">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={severityData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="name" stroke="#64748b" fontSize={12} />
              <YAxis stroke="#64748b" fontSize={12} allowDecimals={false} />
              <Tooltip contentStyle={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 8 }} />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {severityData.map((entry) => (
                  <Cell key={entry.name} fill={CRITICALITY_COLORS[entry.name] ?? "#64748b"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Top Risk Assets">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase text-slate-500">
                <th className="pb-2">Asset</th>
                <th className="pb-2">Score</th>
                <th className="pb-2">Tier</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {data.topRiskAssets.map((a) => (
                <tr key={a.assetId}>
                  <td className="py-2 text-slate-200">{a.assetName}</td>
                  <td className="py-2 font-mono text-slate-300">{a.overallScore.toFixed(1)}</td>
                  <td className="py-2">
                    <Badge value={a.riskTier} />
                  </td>
                </tr>
              ))}
              {data.topRiskAssets.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-slate-500">
                    No risk scores calculated yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>

        <Card title="Top Pending Remediations">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase text-slate-500">
                <th className="pb-2">Vulnerability</th>
                <th className="pb-2">Priority</th>
                <th className="pb-2">Score</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {data.topRemediations.map((r) => (
                <tr key={r.id}>
                  <td className="py-2 text-slate-200">{r.vulnerabilityTitle}</td>
                  <td className="py-2">
                    <Badge value={r.priority} />
                  </td>
                  <td className="py-2 font-mono text-slate-300">{r.priorityScore.toFixed(1)}</td>
                </tr>
              ))}
              {data.topRemediations.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-slate-500">
                    No pending remediations
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>
      </div>
    </div>
  );
}
