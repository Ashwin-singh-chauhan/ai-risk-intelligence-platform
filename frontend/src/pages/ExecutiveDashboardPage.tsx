import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getExecutiveDashboard } from "../api/analytics";
import { useFetch } from "../hooks/useFetch";
import { StatCard } from "../components/StatCard";
import { Card } from "../components/Card";
import { Spinner, ErrorBanner } from "../components/StatusViews";

const TIER_COLORS: Record<string, string> = {
  LOW: "#16a34a",
  MEDIUM: "#eab308",
  HIGH: "#ea580c",
  CRITICAL: "#dc2626",
};

function scoreAccent(score: number): "success" | "warning" | "danger" {
  if (score >= 60) return "danger";
  if (score >= 35) return "warning";
  return "success";
}

export function ExecutiveDashboardPage() {
  const { data, loading, error } = useFetch(getExecutiveDashboard);

  if (loading) return <Spinner />;
  if (error || !data) return <ErrorBanner message={error ?? "Unable to load dashboard"} />;

  const tierData = Object.entries(data.assetCountByRiskTier).map(([name, value]) => ({ name, value }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">Executive Risk Dashboard</h1>
        <p className="text-sm text-slate-500">Enterprise-wide risk posture, business-unit breakdown, and compliance standing.</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          label="Enterprise Risk Score"
          value={data.enterpriseAverageRiskScore.toFixed(1)}
          sublabel="out of 100"
          accent={scoreAccent(data.enterpriseAverageRiskScore)}
        />
        <StatCard label="Open Incidents" value={data.openIncidents} accent="danger" />
        <StatCard label="Critical Open Vulnerabilities" value={data.criticalOpenVulnerabilities} accent="danger" />
        <StatCard
          label="Critical-Tier Assets"
          value={data.assetCountByRiskTier["CRITICAL"] ?? 0}
          accent="danger"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Assets by Risk Tier">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={tierData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="name" stroke="#64748b" fontSize={12} />
              <YAxis stroke="#64748b" fontSize={12} allowDecimals={false} />
              <Tooltip contentStyle={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 8 }} />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {tierData.map((entry) => (
                  <Cell key={entry.name} fill={TIER_COLORS[entry.name] ?? "#64748b"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Compliance Posture">
          <div className="space-y-4">
            {data.compliancePosture.map((c) => (
              <div key={c.framework}>
                <div className="mb-1 flex justify-between text-sm">
                  <span className="text-slate-300">{c.framework.replace(/_/g, " ")}</span>
                  <span className="font-mono text-slate-400">{c.compliancePercentage.toFixed(0)}%</span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                  <div
                    className="h-full rounded-full bg-brand-500"
                    style={{ width: `${Math.min(100, c.compliancePercentage)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card title="Risk by Business Unit">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs uppercase text-slate-500">
              <th className="pb-2">Business Unit</th>
              <th className="pb-2">Average Risk Score</th>
              <th className="pb-2">Asset Count</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {data.riskByBusinessUnit.map((bu) => (
              <tr key={bu.businessUnit}>
                <td className="py-2 text-slate-200">{bu.businessUnit}</td>
                <td className="py-2 font-mono text-slate-300">{bu.averageRiskScore.toFixed(1)}</td>
                <td className="py-2 text-slate-400">{bu.assetCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
