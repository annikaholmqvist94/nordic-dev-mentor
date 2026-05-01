"use client";

import { PERSONALITIES } from "../personality";
import type { PersonalityWire } from "../types";

type Props = {
  active: PersonalityWire;
  onSelect: (wire: PersonalityWire) => void;
};

export function PersonalityTabs({ active, onSelect }: Props) {
  return (
    <div className="px-8 pt-4 border-b border-border-subtle flex shrink-0">
      {PERSONALITIES.map((p) => {
        const isActive = p.wire === active;
        return (
          <button
            key={p.wire}
            onClick={() => onSelect(p.wire)}
            className={`pb-3.5 mr-6 -mb-px text-[11px] uppercase tracking-[0.12em] cursor-pointer border-b-2 transition-colors ${
              isActive
                ? "text-text-primary border-accent"
                : "text-text-disabled border-transparent hover:text-text-muted"
            }`}
          >
            {p.shortLabel}
          </button>
        );
      })}
    </div>
  );
}
