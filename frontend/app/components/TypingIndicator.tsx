import { findPersonality } from "../personality";
import type { PersonalityWire } from "../types";

export function TypingIndicator({
  personality,
}: {
  personality: PersonalityWire;
}) {
  const meta = findPersonality(personality);
  return (
    <div
      role="status"
      aria-live="polite"
      aria-label={`${meta.shortLabel} is composing a response`}
      className="mb-5 pl-4 border-l-2 border-accent"
    >
      <div className="text-[9px] uppercase tracking-[0.16em] text-text-muted font-medium mb-1.5">
        {meta.shortLabel}
      </div>
      <div className="flex items-center gap-2 py-1">
        <div className="flex gap-1" aria-hidden="true">
          <span className="w-1.5 h-1.5 bg-accent rounded-full typing-dot" />
          <span className="w-1.5 h-1.5 bg-accent rounded-full typing-dot delay-150" />
          <span className="w-1.5 h-1.5 bg-accent rounded-full typing-dot delay-300" />
        </div>
        <span className="text-[11px] text-text-muted italic">
          composing a response…
        </span>
      </div>
    </div>
  );
}
