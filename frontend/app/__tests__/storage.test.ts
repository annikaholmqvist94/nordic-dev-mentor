import { describe, it, expect, beforeEach } from "vitest";
import {
  getAllConversations,
  upsertConversation,
  removeConversation,
  findConversation,
} from "../lib/storage";
import type { StoredConversation } from "../types";

const sample = (
  overrides: Partial<StoredConversation> = {},
): StoredConversation => ({
  id: "id-1",
  personality: "senior-architect",
  title: "Postgres or Mongo?",
  lastMessage: "Postgres.",
  updatedAt: 1700000000000,
  ...overrides,
});

describe("conversation storage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("returns empty array when nothing stored", () => {
    expect(getAllConversations()).toEqual([]);
  });

  it("upsert adds a new conversation", () => {
    const conv = sample();
    upsertConversation(conv);
    expect(getAllConversations()).toEqual([conv]);
  });

  it("upsert updates an existing conversation by id", () => {
    upsertConversation(sample());
    upsertConversation(sample({ lastMessage: "updated", updatedAt: 1700000001000 }));
    const all = getAllConversations();
    expect(all).toHaveLength(1);
    expect(all[0].lastMessage).toBe("updated");
  });

  it("getAll sorts by updatedAt descending", () => {
    upsertConversation(sample({ id: "a", updatedAt: 100 }));
    upsertConversation(sample({ id: "b", updatedAt: 300 }));
    upsertConversation(sample({ id: "c", updatedAt: 200 }));
    const ids = getAllConversations().map((c) => c.id);
    expect(ids).toEqual(["b", "c", "a"]);
  });

  it("remove deletes by id", () => {
    upsertConversation(sample({ id: "keep" }));
    upsertConversation(sample({ id: "drop" }));
    removeConversation("drop");
    const ids = getAllConversations().map((c) => c.id);
    expect(ids).toEqual(["keep"]);
  });

  it("remove on non-existent id is a no-op", () => {
    upsertConversation(sample({ id: "keep" }));
    removeConversation("missing");
    expect(getAllConversations()).toHaveLength(1);
  });

  it("findConversation returns the match by id", () => {
    upsertConversation(sample({ id: "abc" }));
    expect(findConversation("abc")?.id).toBe("abc");
    expect(findConversation("nope")).toBeUndefined();
  });

  it("survives malformed JSON in storage", () => {
    localStorage.setItem("ndm.conversations", "not-json{");
    expect(getAllConversations()).toEqual([]);
  });
});
