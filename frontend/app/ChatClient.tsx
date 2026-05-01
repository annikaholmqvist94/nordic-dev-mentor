"use client";

import { useState, useTransition } from "react";
import { sendMessage } from "./actions";
import { DEFAULT_PERSONALITY, findPersonality } from "./personality";
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

export function ChatClient() {
  const [personality, setPersonality] =
    useState<PersonalityWire>(DEFAULT_PERSONALITY);
  const [entries, setEntries] = useState<ConversationEntry[]>([]);
  const [sessionId, setSessionId] = useState<string | undefined>(undefined);
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<ErrorState | null>(null);

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

  function submit(message: string) {
    setError(null);
    setEntries((prev) => [...prev, { id: newId(), kind: "user", content: message }]);

    startTransition(async () => {
      const result = await sendMessage(personality, message, sessionId);
      if (result.ok) {
        setSessionId(result.sessionId);
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
      <PersonalityTabs active={personality} onSelect={selectPersonality} />
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
