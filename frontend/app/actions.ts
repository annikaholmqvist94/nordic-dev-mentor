"use server";

import type { PersonalityWire, SendMessageResult } from "./types";

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

type ChatBackendResponse = {
  sessionId: string;
  personality: string;
  reply: string;
};

export async function sendMessage(
  personality: PersonalityWire,
  message: string,
  sessionId?: string,
): Promise<SendMessageResult> {
  const url = `${process.env.BACKEND_URL}/api/v1/chat`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ personality, message, sessionId }),
      cache: "no-store",
    });

    if (!res.ok) {
      return { ok: false, error: `HTTP ${res.status} from backend` };
    }

    const data: ChatBackendResponse = await res.json();
    return { ok: true, sessionId: data.sessionId, reply: data.reply };
  } catch (err) {
    const error = err instanceof Error ? err.message : "unknown error";
    return { ok: false, error };
  }
}

export async function deleteSession(sessionId: string): Promise<void> {
  const url = `${process.env.BACKEND_URL}/api/v1/chat/${sessionId}`;
  try {
    await fetch(url, { method: "DELETE", cache: "no-store" });
  } catch {
    // best effort — localStorage cleanup proceeds regardless
  }
}
