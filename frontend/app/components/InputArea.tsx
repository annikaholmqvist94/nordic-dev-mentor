"use client";

import { useState, type KeyboardEvent } from "react";

type Props = {
  inputPrompt: string;
  disabled: boolean;
  onSubmit: (message: string) => void;
};

export function InputArea({ inputPrompt, disabled, onSubmit }: Props) {
  const [value, setValue] = useState("");

  function trySubmit() {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSubmit(trimmed);
    setValue("");
  }

  function handleKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      trySubmit();
    }
  }

  return (
    <div className="border-t border-border-subtle bg-cream-surface px-4 sm:px-8 py-3 sm:py-4 flex items-center gap-2 sm:gap-3 shrink-0">
      <span className="font-mono text-[11px] text-text-disabled lowercase shrink-0">
        <span className="text-accent">›</span> {inputPrompt}
      </span>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKey}
        disabled={disabled}
        rows={1}
        aria-label={`Message ${inputPrompt}`}
        className="flex-1 font-mono text-[13px] text-text-primary bg-transparent border-none outline-none resize-none disabled:opacity-50 focus-visible:ring-1 focus-visible:ring-accent focus-visible:rounded-sm"
      />
      <span className="hidden sm:inline text-[9px] uppercase tracking-[0.14em] text-text-disabled shrink-0">
        <kbd className="bg-white border border-border-emphasis border-b-2 px-1.5 py-0.5 font-mono text-[10px] rounded text-text-emphasis">
          ↵
        </kbd>{" "}
        send ·{" "}
        <kbd className="bg-white border border-border-emphasis border-b-2 px-1.5 py-0.5 font-mono text-[10px] rounded text-text-emphasis">
          ⇧↵
        </kbd>{" "}
        newline
      </span>
    </div>
  );
}
