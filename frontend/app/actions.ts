"use server";

type HealthResponse = {
  status: string;
};

export type ProbeResult =
  | { ok: true; status: string }
  | { ok: false; error: string };

export async function probeBackend(): Promise<ProbeResult> {
  const url = `${process.env.BACKEND_URL}/actuator/health`;

  try {
    const res = await fetch(url, { cache: "no-store" });
    if (!res.ok) {
      return { ok: false, error: `HTTP ${res.status} from backend` };
    }
    const data: HealthResponse = await res.json();
    return { ok: true, status: data.status };
  } catch (err) {
    const message = err instanceof Error ? err.message : "unknown error";
    return { ok: false, error: message };
  }
}
