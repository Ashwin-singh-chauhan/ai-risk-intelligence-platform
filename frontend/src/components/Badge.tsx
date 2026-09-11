import clsx from "clsx";

const TIER_STYLES: Record<string, string> = {
  LOW: "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30",
  INFO: "bg-slate-500/15 text-slate-300 ring-slate-500/30",
  MEDIUM: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
  HIGH: "bg-orange-500/15 text-orange-400 ring-orange-500/30",
  CRITICAL: "bg-red-500/15 text-red-400 ring-red-500/30",
  URGENT: "bg-red-500/15 text-red-400 ring-red-500/30",
  OPEN: "bg-sky-500/15 text-sky-400 ring-sky-500/30",
  IN_PROGRESS: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
  INVESTIGATING: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
  CONTAINED: "bg-indigo-500/15 text-indigo-400 ring-indigo-500/30",
  MITIGATED: "bg-indigo-500/15 text-indigo-400 ring-indigo-500/30",
  RESOLVED: "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30",
  CLOSED: "bg-slate-500/15 text-slate-300 ring-slate-500/30",
  ACCEPTED_RISK: "bg-slate-500/15 text-slate-300 ring-slate-500/30",
  PENDING: "bg-sky-500/15 text-sky-400 ring-sky-500/30",
  ACKNOWLEDGED: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
  COMPLETED: "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30",
  DISMISSED: "bg-slate-500/15 text-slate-300 ring-slate-500/30",
  COMPLIANT: "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30",
  PARTIAL: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
  NON_COMPLIANT: "bg-red-500/15 text-red-400 ring-red-500/30",
  NOT_ASSESSED: "bg-slate-500/15 text-slate-300 ring-slate-500/30",
};

export function Badge({ value, className }: { value: string; className?: string }) {
  const style = TIER_STYLES[value] ?? "bg-slate-500/15 text-slate-300 ring-slate-500/30";
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset whitespace-nowrap",
        style,
        className
      )}
    >
      {value.replace(/_/g, " ")}
    </span>
  );
}
