import type { StoredConversation } from "../types";

const KEY = "ndm.conversations";

function read(): StoredConversation[] {
  if (typeof window === "undefined") return [];
  const raw = window.localStorage.getItem(KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as StoredConversation[]) : [];
  } catch {
    return [];
  }
}

function write(list: StoredConversation[]): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(KEY, JSON.stringify(list));
}

export function getAllConversations(): StoredConversation[] {
  return [...read()].sort((a, b) => b.updatedAt - a.updatedAt);
}

export function upsertConversation(conv: StoredConversation): void {
  const list = read();
  const idx = list.findIndex((c) => c.id === conv.id);
  if (idx >= 0) {
    list[idx] = conv;
  } else {
    list.push(conv);
  }
  write(list);
}

export function removeConversation(id: string): void {
  const list = read().filter((c) => c.id !== id);
  write(list);
}

export function findConversation(id: string): StoredConversation | undefined {
  return read().find((c) => c.id === id);
}
