"use client";

import { PERSONALITIES } from "../personality";
import type { PersonalityWire } from "../types";

type Props = {
  active: PersonalityWire;
  onSelect: (wire: PersonalityWire) => void;
  onNewChat?: () => void;
};

export function PersonalityTabs({ active, onSelect, onNewChat }: Props) {
  return (
    <div className="px-8 pt-4 border-b border-border-subtle flex items-end justify-between shrink-0">
      <div className="flex">
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
      {onNewChat && (
        <button
          onClick={onNewChat}
          className="pb-3.5 text-[11px] uppercase tracking-[0.12em] text-text-disabled hover:text-text-primary cursor-pointer transition-colors"
        >
          + New chat
        </button>
      )}
    </div>
  );
}
