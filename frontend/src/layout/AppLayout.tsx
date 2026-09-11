import { NavLink, Outlet, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { useAuthStore } from "../store/authStore";
import { logout as logoutRequest } from "../api/auth";

const NAV_ITEMS = [
  { to: "/analyst", label: "Analyst Dashboard", roles: ["ADMIN", "SECURITY_ANALYST"] },
  { to: "/executive", label: "Executive Dashboard", roles: ["ADMIN", "EXECUTIVE"] },
  { to: "/assets", label: "Assets", roles: ["ADMIN", "SECURITY_ANALYST", "EXECUTIVE", "VIEWER"] },
  { to: "/vulnerabilities", label: "Vulnerabilities", roles: ["ADMIN", "SECURITY_ANALYST", "EXECUTIVE", "VIEWER"] },
  { to: "/incidents", label: "Incidents", roles: ["ADMIN", "SECURITY_ANALYST", "EXECUTIVE", "VIEWER"] },
  { to: "/remediation", label: "Remediation", roles: ["ADMIN", "SECURITY_ANALYST"] },
  { to: "/compliance", label: "Compliance", roles: ["ADMIN", "SECURITY_ANALYST", "EXECUTIVE", "VIEWER"] },
  { to: "/assistant", label: "AI Assistant", roles: ["ADMIN", "SECURITY_ANALYST", "EXECUTIVE", "VIEWER"] },
];

export function AppLayout() {
  const { user, refreshToken, logout } = useAuthStore();
  const navigate = useNavigate();

  const visibleItems = NAV_ITEMS.filter((item) => user && item.roles.includes(user.role));

  async function handleLogout() {
    if (refreshToken) {
      try {
        await logoutRequest(refreshToken);
      } catch {
        // best-effort server-side revoke; proceed with local logout regardless
      }
    }
    logout();
    navigate("/login");
  }

  return (
    <div className="flex min-h-screen bg-slate-950">
      <aside className="flex w-64 flex-col border-r border-slate-800 bg-slate-900/40">
        <div className="px-5 py-6">
          <p className="text-sm font-semibold tracking-wide text-brand-400">ENTERPRISE RISK</p>
          <p className="text-xs text-slate-500">Intelligence Platform</p>
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                clsx(
                  "block rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive ? "bg-brand-600/20 text-brand-300" : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-800 px-4 py-4">
          <p className="truncate text-sm font-medium text-slate-200">{user?.fullName}</p>
          <p className="truncate text-xs text-slate-500">{user?.email}</p>
          <p className="mt-1 text-[10px] font-semibold uppercase tracking-wide text-brand-400">{user?.role.replace(/_/g, " ")}</p>
          <button
            onClick={handleLogout}
            className="mt-3 w-full rounded-lg border border-slate-700 px-3 py-1.5 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            Log out
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto p-8">
        <Outlet />
      </main>
    </div>
  );
}
