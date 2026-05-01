"use client";

import { useState, useTransition } from "react";
import { probeBackend, type ProbeResult } from "./actions";

export default function Home() {
  const [result, setResult] = useState<ProbeResult | null>(null);
  const [pending, startTransition] = useTransition();

  function handleClick() {
    setResult(null);
    startTransition(async () => {
      const r = await probeBackend();
      setResult(r);
    });
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 p-8 font-sans">
      <h1 className="text-3xl font-semibold">Nordic Dev Mentor — frontend smoke test</h1>
      <p className="text-sm text-stone-500 max-w-md text-center">
        Klicka knappen. En Server Action anropar backend via{" "}
        <code className="bg-stone-100 px-1 rounded">{`$BACKEND_URL/actuator/health`}</code>.
      </p>
      <button
        onClick={handleClick}
        disabled={pending}
        className="rounded-md bg-black px-5 py-2 text-white text-sm font-medium disabled:opacity-50"
      >
        {pending ? "Probing…" : "Probe backend"}
      </button>
      {result && (
        <pre className="bg-stone-50 border border-stone-200 rounded-md px-4 py-3 text-sm">
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </main>
  );
}
