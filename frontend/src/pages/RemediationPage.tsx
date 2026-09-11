import { useState } from "react";
import { listRemediations, updateRemediationStatus } from "../api/remediation";
import { useFetch } from "../hooks/useFetch";
import { Card } from "../components/Card";
import { Badge } from "../components/Badge";
import { Spinner, ErrorBanner } from "../components/StatusViews";

const STATUS_OPTIONS = ["PENDING", "ACKNOWLEDGED", "IN_PROGRESS", "COMPLETED", "DISMISSED"];

export function RemediationPage() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);

  const { data, loading, error, reload } = useFetch(
    () => listRemediations({ status: status || undefined, page, size: 15 }),
    [status, page]
  );

  async function handleStatusChange(id: string, newStatus: string) {
    await updateRemediationStatus(id, newStatus);
    reload();
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">Remediation Recommendations</h1>
        <p className="text-sm text-slate-500">Auto-generated, prioritized guidance for open vulnerabilities.</p>
      </div>

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

      <div className="space-y-3">
        {loading ? (
          <Spinner />
        ) : error || !data ? (
          <ErrorBanner message={error ?? "Failed to load remediations"} />
        ) : data.content.length === 0 ? (
          <Card>
            <p className="text-sm text-slate-500">No remediation recommendations match this filter.</p>
          </Card>
        ) : (
          data.content.map((r) => (
            <Card key={r.id}>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge value={r.priority} />
                    <span className="text-xs text-slate-500">Priority score {r.priorityScore.toFixed(1)}</span>
                  </div>
                  <p className="mt-2 font-medium text-slate-200">{r.vulnerabilityTitle}</p>
                  <p className="text-xs text-slate-500">Asset: {r.assetName}</p>
                  <p className="mt-2 text-sm leading-relaxed text-slate-400">{r.recommendation}</p>
                  <p className="mt-2 text-xs text-slate-500">Estimated effort: {r.estimatedEffortHours}h</p>
                </div>
                <select
                  value={r.status}
                  onChange={(e) => handleStatusChange(r.id, e.target.value)}
                  className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-200"
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
            </Card>
          ))
        )}
      </div>

      {data && (
        <div className="flex items-center justify-between text-xs text-slate-500">
          <span>
            Page {data.page + 1} of {Math.max(1, data.totalPages)} - {data.totalElements} recommendations
          </span>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="rounded border border-slate-700 px-2 py-1 disabled:opacity-40">
              Prev
            </button>
            <button disabled={data.last} onClick={() => setPage((p) => p + 1)} className="rounded border border-slate-700 px-2 py-1 disabled:opacity-40">
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
