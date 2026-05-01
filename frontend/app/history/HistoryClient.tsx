"use client";

import { useEffect, useState, useTransition } from "react";
import Link from "next/link";
import {
  getAllConversations,
  removeConversation,
} from "../lib/storage";
import { deleteSession } from "../actions";
import { findPersonality } from "../personality";
import type { StoredConversation } from "../types";

function relativeTime(timestamp: number): string {
  const diff = Date.now() - timestamp;
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export function HistoryClient() {
  const [list, setList] = useState<StoredConversation[]>([]);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  useEffect(() => {
    setList(getAllConversations());
  }, []);

  function confirmDelete(id: string) {
    startTransition(async () => {
      removeConversation(id);
      await deleteSession(id);
      setList(getAllConversations());
      setPendingDeleteId(null);
    });
  }

  if (list.length === 0) {
    return (
      <p className="text-[14px] text-text-muted leading-[1.65]">
        No saved conversations yet.{" "}
        <Link href="/" className="text-accent hover:underline">
          Start one
        </Link>
        .
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-1">
      {list.map((conv) => {
        const meta = findPersonality(conv.personality);
        const isPending = pendingDeleteId === conv.id;
        return (
          <li
            key={conv.id}
            className="border-b border-border-subtle py-4 flex items-start justify-between gap-4 last:border-b-0"
          >
            <Link
              href={`/?id=${conv.id}`}
              className="flex-1 min-w-0 group"
            >
              <div className="font-serif text-[16px] text-text-primary group-hover:text-accent transition-colors leading-snug truncate">
                {conv.title}
              </div>
              <div className="text-[10px] uppercase tracking-[0.12em] text-text-muted mt-1">
                {meta.shortLabel} · {relativeTime(conv.updatedAt)}
              </div>
            </Link>
            {isPending ? (
              <div className="flex gap-2 shrink-0 text-[10px] uppercase tracking-[0.12em]">
                <button
                  onClick={() => confirmDelete(conv.id)}
                  className="text-error-accent hover:underline cursor-pointer"
                >
                  Confirm
                </button>
                <button
                  onClick={() => setPendingDeleteId(null)}
                  className="text-text-muted hover:underline cursor-pointer"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                onClick={() => setPendingDeleteId(conv.id)}
                className="shrink-0 text-text-disabled hover:text-error-accent text-[16px] leading-none cursor-pointer transition-colors"
                aria-label="Delete conversation"
              >
                ×
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}
