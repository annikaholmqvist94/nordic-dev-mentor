"use client";

import { useState, useTransition, useEffect } from "react";
import { sendMessage } from "./actions";
import { DEFAULT_PERSONALITY, findPersonality } from "./personality";
import {
  findConversation,
  upsertConversation,
} from "./lib/storage";
import type { ConversationEntry, PersonalityWire } from "./types";
import { TopBar } from "./components/TopBar";
import { PersonalityTabs } from "./components/PersonalityTabs";
import { ReadingColumn } from "./components/ReadingColumn";
import { Message } from "./components/Message";
import { EmptyState } from "./components/EmptyState";
import { TypingIndicator } from "./components/TypingIndicator";
import { ErrorBlock } from "./components/ErrorBlock";
import { SwitchDivider } from "./components/SwitchDivider";
import { InputArea } from "./components/InputArea";

type ErrorState = {
  retryMessage: string;
};

let entryIdCounter = 0;
const newId = () => `e-${++entryIdCounter}`;

type Props = {
  initialSessionId?: string;
};

export function ChatClient({ initialSessionId }: Props) {
  const [personality, setPersonality] =
    useState<PersonalityWire>(DEFAULT_PERSONALITY);
  const [entries, setEntries] = useState<ConversationEntry[]>([]);
  const [sessionId, setSessionId] = useState<string | undefined>(
    initialSessionId,
  );
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<ErrorState | null>(null);

  // Hydrate personality from localStorage when continuing an existing session.
  useEffect(() => {
    if (!initialSessionId) return;
    const stored = findConversation(initialSessionId);
    if (stored) {
      setPersonality(stored.personality);
    }
  }, [initialSessionId]);

  function selectPersonality(next: PersonalityWire) {
    if (next === personality) return;
    if (entries.length > 0) {
      setEntries((prev) => [
        ...prev,
        { id: newId(), kind: "switch", from: personality, to: next },
      ]);
    }
    setPersonality(next);
    setError(null);
  }

  function newChat() {
    setEntries([]);
    setSessionId(undefined);
    setError(null);
    setPersonality(DEFAULT_PERSONALITY);
    if (typeof window !== "undefined" && window.location.search) {
      window.history.replaceState({}, "", "/");
    }
  }

  function persistConversation(id: string, userMsg: string) {
    const existing = findConversation(id);
    upsertConversation({
      id,
      personality,
      title: existing?.title ?? userMsg.slice(0, 60),
      lastMessage: userMsg,
      updatedAt: Date.now(),
    });
  }

  function submit(message: string) {
    setError(null);
    setEntries((prev) => [...prev, { id: newId(), kind: "user", content: message }]);

    startTransition(async () => {
      const result = await sendMessage(personality, message, sessionId);
      if (result.ok) {
        setSessionId(result.sessionId);
        persistConversation(result.sessionId, message);
        setEntries((prev) => [
          ...prev,
          {
            id: newId(),
            kind: "assistant",
            content: result.reply,
            personality,
          },
        ]);
      } else {
        setError({ retryMessage: message });
      }
    });
  }

  function retry() {
    if (!error) return;
    submit(error.retryMessage);
  }

  const meta = findPersonality(personality);

  return (
    <div className="flex flex-col min-h-screen">
      <TopBar />
      <PersonalityTabs
        active={personality}
        onSelect={selectPersonality}
        onNewChat={newChat}
      />
      <ReadingColumn>
        {entries.length === 0 && !pending && !error ? (
          <EmptyState personality={personality} />
        ) : (
          <>
            {entries.map((entry) => {
              if (entry.kind === "user") {
                return (
                  <Message
                    key={entry.id}
                    kind="user"
                    content={entry.content}
                  />
                );
              }
              if (entry.kind === "assistant") {
                return (
                  <Message
                    key={entry.id}
                    kind="assistant"
                    content={entry.content}
                    personality={entry.personality}
                  />
                );
              }
              return <SwitchDivider key={entry.id} to={entry.to} />;
            })}
            {pending && <TypingIndicator personality={personality} />}
            {error && (
              <ErrorBlock
                personalityFullLabel={meta.fullLabel}
                onRetry={retry}
              />
            )}
          </>
        )}
      </ReadingColumn>
      <InputArea
        inputPrompt={meta.inputPrompt}
        disabled={pending}
        onSubmit={submit}
      />
    </div>
  );
}
