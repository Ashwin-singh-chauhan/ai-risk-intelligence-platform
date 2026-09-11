import { useState } from "react";
import { getCompliancePosture, listComplianceControls } from "../api/compliance";
import { useFetch } from "../hooks/useFetch";
import { Card } from "../components/Card";
import { Badge } from "../components/Badge";
import { Spinner, ErrorBanner } from "../components/StatusViews";
import type { ComplianceFramework } from "../types/domain";

const FRAMEWORKS: ComplianceFramework[] = ["NIST_CSF", "ISO_27001", "SOC2"];

export function CompliancePage() {
  const [framework, setFramework] = useState<ComplianceFramework>("NIST_CSF");

  const { data: controls, loading, error } = useFetch(() => listComplianceControls(framework), [framework]);
  const { data: posture } = useFetch(() => getCompliancePosture(framework), [framework]);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">Compliance</h1>
        <p className="text-sm text-slate-500">Conceptual mapping to NIST CSF, ISO 27001, and SOC 2 controls.</p>
      </div>

      <div className="flex gap-2">
        {FRAMEWORKS.map((f) => (
          <button
            key={f}
            onClick={() => setFramework(f)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium ${
              framework === f ? "bg-brand-600 text-white" : "border border-slate-700 text-slate-300 hover:bg-slate-800"
            }`}
          >
            {f.replace(/_/g, " ")}
          </button>
        ))}
      </div>

      {posture && (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-5">
          <Card>
            <p className="text-xs text-slate-500">Overall Posture</p>
            <p className="mt-1 text-2xl font-semibold text-slate-100">{posture.compliancePercentage.toFixed(0)}%</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Compliant</p>
            <p className="mt-1 text-2xl font-semibold text-emerald-400">{posture.compliant}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Partial</p>
            <p className="mt-1 text-2xl font-semibold text-amber-400">{posture.partial}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Non-Compliant</p>
            <p className="mt-1 text-2xl font-semibold text-red-400">{posture.nonCompliant}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Not Assessed</p>
            <p className="mt-1 text-2xl font-semibold text-slate-400">{posture.notAssessed}</p>
          </Card>
        </div>
      )}

      <Card title="Controls">
        {loading ? (
          <Spinner />
        ) : error || !controls ? (
          <ErrorBanner message={error ?? "Failed to load controls"} />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase text-slate-500">
                <th className="pb-2">Control</th>
                <th className="pb-2">Category</th>
                <th className="pb-2">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {controls.map((c) => (
                <tr key={c.controlId}>
                  <td className="py-2 text-slate-200">
                    <div className="font-mono text-xs text-slate-500">{c.controlIdentifier}</div>
                    <div>{c.controlName}</div>
                  </td>
                  <td className="py-2 text-slate-400">{c.category}</td>
                  <td className="py-2">
                    <Badge value={c.latestStatus} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
