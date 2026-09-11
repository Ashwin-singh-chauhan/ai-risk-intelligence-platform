import { useState } from "react";
import { searchAssets } from "../api/assets";
import { getLatestRiskForAsset, recomputeRisk } from "../api/risk";
import { useFetch } from "../hooks/useFetch";
import { Card } from "../components/Card";
import { Badge } from "../components/Badge";
import { Spinner, ErrorBanner } from "../components/StatusViews";
import type { RiskScore } from "../types/domain";
import { useAuthStore } from "../store/authStore";

export function AssetsPage() {
  const [query, setQuery] = useState("");
  const [criticality, setCriticality] = useState("");
  const [page, setPage] = useState(0);
  const [selectedAssetId, setSelectedAssetId] = useState<string | null>(null);
  const [riskScore, setRiskScore] = useState<RiskScore | null>(null);
  const [riskLoading, setRiskLoading] = useState(false);
  const canRecompute = useAuthStore((s) => s.hasAnyRole(["ADMIN", "SECURITY_ANALYST"]));

  const { data, loading, error } = useFetch(
    () => searchAssets({ query: query || undefined, criticality: criticality || undefined, page, size: 15 }),
    [query, criticality, page]
  );

  async function viewRisk(assetId: string) {
    setSelectedAssetId(assetId);
    setRiskLoading(true);
    try {
      const score = await getLatestRiskForAsset(assetId);
      setRiskScore(score);
    } catch {
      setRiskScore(null);
    } finally {
      setRiskLoading(false);
    }
  }

  async function handleRecompute(assetId: string) {
    setRiskLoading(true);
    const score = await recomputeRisk(assetId);
    setRiskScore(score);
    setRiskLoading(false);
  }

  return (
    <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
      <div className="space-y-4 xl:col-span-2">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">Asset Inventory</h1>
          <p className="text-sm text-slate-500">Business-critical assets with exposure and criticality classification.</p>
        </div>

        <div className="flex gap-3">
          <input
            placeholder="Search by name or tag..."
            value={query}
            onChange={(e) => {
              setPage(0);
              setQuery(e.target.value);
            }}
            className="flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
          />
          <select
            value={criticality}
            onChange={(e) => {
              setPage(0);
              setCriticality(e.target.value);
            }}
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
          >
            <option value="">All criticality</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        <Card>
          {loading ? (
            <Spinner />
          ) : error || !data ? (
            <ErrorBanner message={error ?? "Failed to load assets"} />
          ) : (
            <>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs uppercase text-slate-500">
                    <th className="pb-2">Name</th>
                    <th className="pb-2">Type</th>
                    <th className="pb-2">Business Unit</th>
                    <th className="pb-2">Criticality</th>
                    <th className="pb-2">Exposure</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {data.content.map((asset) => (
                    <tr
                      key={asset.id}
                      onClick={() => viewRisk(asset.id)}
                      className={`cursor-pointer hover:bg-slate-800/50 ${selectedAssetId === asset.id ? "bg-slate-800/60" : ""}`}
                    >
                      <td className="py-2 text-slate-200">
                        {asset.name}
                        <div className="text-xs text-slate-500">{asset.assetTag}</div>
                      </td>
                      <td className="py-2 text-slate-400">{asset.assetType}</td>
                      <td className="py-2 text-slate-400">{asset.businessUnit}</td>
                      <td className="py-2">
                        <Badge value={asset.criticality} />
                      </td>
                      <td className="py-2">
                        <Badge value={asset.exposure} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
                <span>
                  Page {data.page + 1} of {Math.max(1, data.totalPages)} - {data.totalElements} assets
                </span>
                <div className="flex gap-2">
                  <button
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="rounded border border-slate-700 px-2 py-1 disabled:opacity-40"
                  >
                    Prev
                  </button>
                  <button
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                    className="rounded border border-slate-700 px-2 py-1 disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          )}
        </Card>
      </div>

      <div>
        <Card title="Risk Score Explanation">
          {!selectedAssetId ? (
            <p className="text-sm text-slate-500">Select an asset to view its explainable risk score.</p>
          ) : riskLoading ? (
            <Spinner />
          ) : !riskScore ? (
            <div className="space-y-3">
              <p className="text-sm text-slate-500">No risk score calculated yet for this asset.</p>
              {canRecompute && (
                <button
                  onClick={() => handleRecompute(selectedAssetId)}
                  className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-500"
                >
                  Compute now
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-3xl font-semibold text-slate-100">{riskScore.overallScore.toFixed(1)}</p>
                  <p className="text-xs text-slate-500">Overall risk score / 100</p>
                </div>
                <Badge value={riskScore.riskTier} />
              </div>

              <div className="space-y-2">
                {[
                  ["Vulnerability", riskScore.vulnerabilityComponent, 0.3],
                  ["Exposure", riskScore.exposureComponent, 0.2],
                  ["Threat Activity", riskScore.threatActivityComponent, 0.2],
                  ["Compliance", riskScore.complianceComponent, 0.15],
                  ["Incident History", riskScore.incidentHistoryComponent, 0.15],
                ].map(([label, value, weight]) => (
                  <div key={label as string}>
                    <div className="mb-1 flex justify-between text-xs text-slate-400">
                      <span>
                        {label} ({Math.round((weight as number) * 100)}% weight)
                      </span>
                      <span className="font-mono">{(value as number).toFixed(1)}</span>
                    </div>
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-800">
                      <div className="h-full rounded-full bg-brand-500" style={{ width: `${value}%` }} />
                    </div>
                  </div>
                ))}
              </div>

              <p className="rounded-lg bg-slate-800/50 p-3 text-xs leading-relaxed text-slate-300">
                {riskScore.explanation?.narrative}
              </p>

              {riskScore.explanation?.topContributingVulnerabilities?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-slate-400">Top contributing vulnerabilities</p>
                  <ul className="space-y-1 text-xs text-slate-400">
                    {riskScore.explanation.topContributingVulnerabilities.map((v) => (
                      <li key={v.id}>
                        {v.cveId ?? "N/A"} - CVSS {v.cvssScore} {v.exploitAvailable ? "(exploit available)" : ""}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {canRecompute && (
                <button
                  onClick={() => handleRecompute(selectedAssetId)}
                  className="w-full rounded-lg border border-slate-700 px-3 py-1.5 text-xs font-medium text-slate-300 hover:bg-slate-800"
                >
                  Recompute now
                </button>
              )}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
