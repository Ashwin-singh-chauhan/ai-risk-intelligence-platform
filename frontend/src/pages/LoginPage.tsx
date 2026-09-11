import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { useAuthStore } from "../store/authStore";

const DEMO_ACCOUNTS = [
  { label: "Admin", email: "admin@erip.com" },
  { label: "Security Analyst", email: "analyst@erip.com" },
  { label: "Executive", email: "exec@erip.com" },
  { label: "Viewer", email: "viewer@erip.com" },
];

export function LoginPage() {
  const [email, setEmail] = useState("analyst@erip.com");
  const [password, setPassword] = useState("Passw0rd!123");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const auth = await login(email, password);
      setSession(auth);
      navigate("/analyst");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Invalid credentials";
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900/60 p-8 shadow-xl">
        <p className="text-sm font-semibold tracking-wide text-brand-400">ENTERPRISE RISK INTELLIGENCE</p>
        <h1 className="mt-1 text-2xl font-semibold text-slate-100">Sign in to your workspace</h1>
        <p className="mt-2 text-sm text-slate-500">
          AI-powered cybersecurity risk platform - assets, vulnerabilities, incidents, and explainable risk scoring.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-400">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-400">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
            />
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-600 px-3 py-2 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:opacity-50"
          >
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <div className="mt-6 border-t border-slate-800 pt-4">
          <p className="mb-2 text-xs font-medium text-slate-500">Demo accounts (password: Passw0rd!123)</p>
          <div className="flex flex-wrap gap-2">
            {DEMO_ACCOUNTS.map((acct) => (
              <button
                key={acct.email}
                type="button"
                onClick={() => setEmail(acct.email)}
                className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:border-brand-500 hover:text-brand-300"
              >
                {acct.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
