import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("../actions", () => ({
  sendMessage: vi.fn(),
}));

import { sendMessage } from "../actions";
import { ChatClient } from "../ChatClient";

describe("ChatClient", () => {
  beforeEach(() => {
    vi.mocked(sendMessage).mockReset();
    localStorage.clear();
  });

  it("renders the empty state for the default personality on first load", () => {
    render(<ChatClient />);
    expect(screen.getByText("The architect is listening.")).toBeInTheDocument();
  });

  it("sends a message and renders the user + assistant entries", async () => {
    vi.mocked(sendMessage).mockResolvedValueOnce({
      ok: true,
      sessionId: "s-1",
      reply: "Postgres.",
    });

    const user = userEvent.setup();
    render(<ChatClient />);

    const textarea = screen.getByRole("textbox");
    await user.type(textarea, "DB?");
    await user.keyboard("{Enter}");

    expect(screen.getByText("DB?")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByText("Postgres.")).toBeInTheDocument(),
    );
    expect(sendMessage).toHaveBeenCalledWith(
      "senior-architect",
      "DB?",
      undefined,
    );
  });

  it("reuses the sessionId returned by the first reply on the next message", async () => {
    vi.mocked(sendMessage)
      .mockResolvedValueOnce({ ok: true, sessionId: "s-1", reply: "first" })
      .mockResolvedValueOnce({ ok: true, sessionId: "s-1", reply: "second" });

    const user = userEvent.setup();
    render(<ChatClient />);

    const textarea = screen.getByRole("textbox");
    await user.type(textarea, "one");
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByText("first")).toBeInTheDocument(),
    );

    await user.type(textarea, "two");
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByText("second")).toBeInTheDocument(),
    );

    expect(sendMessage).toHaveBeenLastCalledWith(
      "senior-architect",
      "two",
      "s-1",
    );
  });

  it("renders an error block with retry when sendMessage fails", async () => {
    vi.mocked(sendMessage).mockResolvedValueOnce({
      ok: false,
      error: "ENOTFOUND",
    });

    const user = userEvent.setup();
    render(<ChatClient />);

    const textarea = screen.getByRole("textbox");
    await user.type(textarea, "ping");
    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument(),
    );
  });

  it("renders a switch divider when personality changes mid-conversation", async () => {
    vi.mocked(sendMessage).mockResolvedValueOnce({
      ok: true,
      sessionId: "s-1",
      reply: "Postgres.",
    });

    const user = userEvent.setup();
    render(<ChatClient />);

    await user.type(screen.getByRole("textbox"), "DB?");
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByText("Postgres.")).toBeInTheDocument(),
    );

    await user.click(screen.getByRole("button", { name: /reviewer/i }));

    expect(screen.getByText(/switched to/i)).toBeInTheDocument();
    expect(screen.getByText(/the reviewer/i)).toBeInTheDocument();
  });

  it("persists conversation metadata to localStorage on first reply", async () => {
    vi.mocked(sendMessage).mockResolvedValueOnce({
      ok: true,
      sessionId: "session-xyz",
      reply: "Postgres.",
    });

    const user = userEvent.setup();
    render(<ChatClient />);

    await user.type(screen.getByRole("textbox"), "DB?");
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByText("Postgres.")).toBeInTheDocument(),
    );

    const raw = localStorage.getItem("ndm.conversations");
    expect(raw).not.toBeNull();
    const parsed = JSON.parse(raw!);
    expect(parsed).toHaveLength(1);
    expect(parsed[0]).toMatchObject({
      id: "session-xyz",
      personality: "senior-architect",
      title: "DB?",
      lastMessage: "DB?",
    });
    expect(typeof parsed[0].updatedAt).toBe("number");
  });

  it("clears state when newChat button is clicked after a conversation", async () => {
    vi.mocked(sendMessage).mockResolvedValueOnce({
      ok: true,
      sessionId: "session-1",
      reply: "first reply",
    });

    const user = userEvent.setup();
    render(<ChatClient />);

    await user.type(screen.getByRole("textbox"), "first message");
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByText("first reply")).toBeInTheDocument(),
    );

    await user.click(screen.getByRole("button", { name: /new chat/i }));

    expect(screen.queryByText("first message")).not.toBeInTheDocument();
    expect(screen.queryByText("first reply")).not.toBeInTheDocument();
    expect(screen.getByText("The architect is listening.")).toBeInTheDocument();
  });
});
