import { type FormEvent, useState } from "react";
import { askAssistant } from "../api/rag";
import { Card } from "../components/Card";
import type { AskResponse } from "../types/domain";

interface ChatTurn {
  question: string;
  response: AskResponse | null;
  loading: boolean;
}

const SUGGESTIONS = [
  "Which assets have the highest risk score right now?",
  "What critical vulnerabilities are still unpatched?",
  "Summarize our open security incidents.",
  "How compliant are we with ISO 27001 access control?",
];

export function AssistantPage() {
  const [question, setQuestion] = useState("");
  const [turns, setTurns] = useState<ChatTurn[]>([]);

  async function ask(q: string) {
    if (!q.trim()) return;
    setQuestion("");
    setTurns((prev) => [...prev, { question: q, response: null, loading: true }]);
    try {
      const response = await askAssistant(q);
      setTurns((prev) => prev.map((t) => (t.question === q && t.loading ? { ...t, response, loading: false } : t)));
    } catch {
      setTurns((prev) =>
        prev.map((t) =>
          t.question === q && t.loading
            ? { ...t, loading: false, response: { answer: "Something went wrong. Please try again.", llmProvider: "n/a", referencedDocuments: [] } }
            : t
        )
      );
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    ask(question);
  }

  return (
    <div className="mx-auto flex h-full max-w-3xl flex-col space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-100">AI Security Assistant</h1>
        <p className="text-sm text-slate-500">
          Retrieval-augmented assistant grounded in live asset, vulnerability, incident and risk data.
        </p>
      </div>

      {turns.length === 0 && (
        <Card title="Try asking">
          <div className="flex flex-wrap gap-2">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => ask(s)}
                className="rounded-full border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:border-brand-500 hover:text-brand-300"
              >
                {s}
              </button>
            ))}
          </div>
        </Card>
      )}

      <div className="flex-1 space-y-4 overflow-y-auto">
        {turns.map((turn, idx) => (
          <div key={idx} className="space-y-2">
            <div className="ml-auto max-w-md rounded-2xl rounded-br-sm bg-brand-600 px-4 py-2 text-sm text-white">
              {turn.question}
            </div>
            <div className="max-w-xl rounded-2xl rounded-bl-sm border border-slate-800 bg-slate-900/60 px-4 py-3 text-sm text-slate-200">
              {turn.loading ? (
                <span className="text-slate-500">Thinking...</span>
              ) : (
                <>
                  <p className="whitespace-pre-wrap leading-relaxed">{turn.response?.answer}</p>
                  {turn.response && turn.response.referencedDocuments.length > 0 && (
                    <div className="mt-3 border-t border-slate-800 pt-2">
                      <p className="mb-1 text-xs font-medium text-slate-500">Grounded references</p>
                      <ul className="space-y-1 text-xs text-slate-500">
                        {turn.response.referencedDocuments.map((doc, i) => (
                          <li key={i}>
                            [{doc.sourceType}] {doc.title}{" "}
                            <span className="font-mono text-slate-600">({(doc.similarity * 100).toFixed(0)}% match)</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  {turn.response && (
                    <p className="mt-2 text-[10px] uppercase tracking-wide text-slate-600">via {turn.response.llmProvider}</p>
                  )}
                </>
              )}
            </div>
          </div>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask about assets, vulnerabilities, incidents, or compliance..."
          className="flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
        />
        <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-500">
          Ask
        </button>
      </form>
    </div>
  );
}
