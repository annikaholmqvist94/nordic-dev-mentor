import { findPersonality } from "../personality";
import type { PersonalityWire } from "../types";

export function EmptyState({ personality }: { personality: PersonalityWire }) {
  const meta = findPersonality(personality);
  return (
    <div className="max-w-[480px] mx-auto mt-16 text-center">
      <h2 className="font-serif text-[32px] font-medium leading-[1.2] mb-3">
        {meta.emptyHeading}
      </h2>
      <p className="font-serif italic text-[18px] text-accent leading-[1.4] border-y border-border-subtle py-4 my-6">
        &ldquo;{meta.emptyQuote}&rdquo;
      </p>
      <p className="text-[11px] uppercase tracking-[0.12em] text-text-disabled">
        Start with a question
      </p>
    </div>
  );
}
