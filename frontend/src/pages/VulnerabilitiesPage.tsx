import { useState } from "react";
import { searchVulnerabilities, updateVulnerabilityStatus } from "../api/vulnerabilities";
import { useFetch } from "../hooks/useFetch";
import { Card } from "../components/Card";
import { Badge } from "../components/Badge";
import { Spinner, ErrorBanner } from "../components/StatusViews";
import { useAuthStore } from "../store/authStore";

const STATUS_OPTIONS = ["OPEN", "IN_PROGRESS", "MITIGATED", "RESOLVED", "ACCEPTED_RISK"];

export function VulnerabilitiesPage() {
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const canManage = useAuthStore((s) => s.hasAnyRole(["ADMIN", "SECURITY_ANALYST"]));

  const { data, loading, error, reload } = useFetch(
    () => searchVulnerabilities({ severity: severity || undefined, status: status || undefined, page, size: 15 }),
    [severity, status, page]
  );

  async function handleStatusChange(id: string, newStatus: string) {
    await updateVulnerabilityStatus(id, newStatus);
    reload();
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">Vulnerability Management</h1>
        <p className="text-sm text-slate-500">CVE/CVSS-tracked findings across the asset inventory.</p>
      </div>

      <div className="flex gap-3">
        <select
          value={severity}
          onChange={(e) => {
            setPage(0);
            setSeverity(e.target.value);
          }}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
        >
          <option value="">All severities</option>
          {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select
          value={status}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value);
          }}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <Card>
        {loading ? (
          <Spinner />
        ) : error || !data ? (
          <ErrorBanner message={error ?? "Failed to load vulnerabilities"} />
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-slate-500">
                  <th className="pb-2">CVE / Title</th>
                  <th className="pb-2">Asset</th>
                  <th className="pb-2">CVSS</th>
                  <th className="pb-2">Severity</th>
                  <th className="pb-2">Status</th>
                  {canManage && <th className="pb-2">Update</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {data.content.map((v) => (
                  <tr key={v.id}>
                    <td className="max-w-xs py-2 text-slate-200">
                      <div className="font-mono text-xs text-slate-400">{v.cveId ?? "N/A"}</div>
                      <div className="truncate">{v.title}</div>
                    </td>
                    <td className="py-2 text-slate-400">{v.assetName}</td>
                    <td className="py-2 font-mono text-slate-300">{v.cvssScore}</td>
                    <td className="py-2">
                      <Badge value={v.severity} />
                    </td>
                    <td className="py-2">
                      <Badge value={v.status} />
                    </td>
                    {canManage && (
                      <td className="py-2">
                        <select
                          defaultValue=""
                          onChange={(e) => e.target.value && handleStatusChange(v.id, e.target.value)}
                          className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-200"
                        >
                          <option value="" disabled>
                            Change...
                          </option>
                          {STATUS_OPTIONS.map((s) => (
                            <option key={s} value={s}>
                              {s}
                            </option>
                          ))}
                        </select>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
              <span>
                Page {data.page + 1} of {Math.max(1, data.totalPages)} - {data.totalElements} vulnerabilities
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
  );
}
