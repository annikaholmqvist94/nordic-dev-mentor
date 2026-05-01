import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";

const ORIGINAL_FETCH = global.fetch;
const ORIGINAL_BACKEND_URL = process.env.BACKEND_URL;

describe("deleteSession", () => {
  beforeEach(() => {
    process.env.BACKEND_URL = "http://localhost:8080";
    global.fetch = vi.fn();
  });

  afterEach(() => {
    global.fetch = ORIGINAL_FETCH;
    process.env.BACKEND_URL = ORIGINAL_BACKEND_URL;
    vi.restoreAllMocks();
  });

  it("DELETEs to the backend by sessionId", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(null, { status: 204 }),
    );

    const { deleteSession } = await import("../actions");
    await deleteSession("abc-123");

    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/chat/abc-123",
      expect.objectContaining({
        method: "DELETE",
        cache: "no-store",
      }),
    );
  });

  it("does not throw when backend returns non-204", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response("not found", { status: 404 }),
    );
    const { deleteSession } = await import("../actions");

    await expect(deleteSession("abc-123")).resolves.toBeUndefined();
  });

  it("does not throw when fetch fails (best-effort)", async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error("network down"),
    );
    const { deleteSession } = await import("../actions");

    await expect(deleteSession("abc-123")).resolves.toBeUndefined();
  });
});
