import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";

const ORIGINAL_FETCH = global.fetch;
const ORIGINAL_BACKEND_URL = process.env.BACKEND_URL;

describe("sendMessage", () => {
  beforeEach(() => {
    process.env.BACKEND_URL = "http://localhost:8080";
    global.fetch = vi.fn();
  });

  afterEach(() => {
    global.fetch = ORIGINAL_FETCH;
    process.env.BACKEND_URL = ORIGINAL_BACKEND_URL;
    vi.restoreAllMocks();
  });

  it("posts personality, message, and optional sessionId to backend", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          sessionId: "abc-123",
          personality: "senior-architect",
          reply: "Postgres.",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const { sendMessage } = await import("../actions");
    const result = await sendMessage("senior-architect", "DB?", undefined);

    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/chat",
      expect.objectContaining({
        method: "POST",
        cache: "no-store",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
        }),
      }),
    );
    const callArgs = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(JSON.parse(callArgs[1].body)).toEqual({
      personality: "senior-architect",
      message: "DB?",
    });
    expect(result).toEqual({
      ok: true,
      sessionId: "abc-123",
      reply: "Postgres.",
    });
  });

  it("returns ok:false when backend returns non-2xx", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response("backend down", { status: 503 }),
    );
    const { sendMessage } = await import("../actions");

    const result = await sendMessage("senior-architect", "ping");

    expect(result).toEqual({ ok: false, error: "HTTP 503 from backend" });
  });

  it("returns ok:false when fetch throws", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error("ENOTFOUND"),
    );
    const { sendMessage } = await import("../actions");

    const result = await sendMessage("senior-architect", "ping");

    expect(result).toEqual({ ok: false, error: "ENOTFOUND" });
  });

  it("includes sessionId in request body when provided", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          sessionId: "existing-session",
          personality: "senior-architect",
          reply: "follow-up reply",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const { sendMessage } = await import("../actions");
    await sendMessage("senior-architect", "follow-up", "existing-session");

    const callArgs = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(JSON.parse(callArgs[1].body)).toEqual({
      personality: "senior-architect",
      message: "follow-up",
      sessionId: "existing-session",
    });
  });
});
