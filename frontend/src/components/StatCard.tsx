import type { ReactNode } from "react";
import clsx from "clsx";

interface StatCardProps {
  label: string;
  value: ReactNode;
  sublabel?: string;
  accent?: "default" | "danger" | "warning" | "success";
}

const ACCENTS: Record<NonNullable<StatCardProps["accent"]>, string> = {
  default: "text-slate-100",
  danger: "text-red-400",
  warning: "text-amber-400",
  success: "text-emerald-400",
};

export function StatCard({ label, value, sublabel, accent = "default" }: StatCardProps) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <p className={clsx("mt-2 text-3xl font-semibold", ACCENTS[accent])}>{value}</p>
      {sublabel && <p className="mt-1 text-xs text-slate-500">{sublabel}</p>}
    </div>
  );
}
