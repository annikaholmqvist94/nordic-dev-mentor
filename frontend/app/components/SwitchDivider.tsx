import { findPersonality } from "../personality";
import type { PersonalityWire } from "../types";

export function SwitchDivider({ to }: { to: PersonalityWire }) {
  const meta = findPersonality(to);
  return (
    <div className="text-center my-6 relative text-[9px] uppercase tracking-[0.18em] text-text-disabled">
      <span className="bg-cream relative z-10 px-3">
        switched to <em className="not-italic font-medium text-accent">{meta.fullLabel}</em>
      </span>
      <div className="absolute top-1/2 left-0 right-0 h-px bg-border-subtle -z-0" />
    </div>
  );
}
