import type { PersonalityWire } from "./types";

export type PersonalityMeta = {
  wire: PersonalityWire;
  shortLabel: string;
  fullLabel: string;
  emptyHeading: string;
  emptyQuote: string;
  inputPrompt: string;
};

export const PERSONALITIES: PersonalityMeta[] = [
  {
    wire: "junior-helper",
    shortLabel: "Junior",
    fullLabel: "the junior helper",
    emptyHeading: "The junior helper is here.",
    emptyQuote: "There are no stupid questions — start anywhere.",
    inputPrompt: "message junior-helper",
  },
  {
    wire: "senior-architect",
    shortLabel: "Architect",
    fullLabel: "the architect",
    emptyHeading: "The architect is listening.",
    emptyQuote:
      "Before I commit to a solution, tell me what you're actually optimizing for.",
    inputPrompt: "message senior-architect",
  },
  {
    wire: "code-reviewer",
    shortLabel: "Reviewer",
    fullLabel: "the reviewer",
    emptyHeading: "The reviewer is ready.",
    emptyQuote: "Paste the code. I'll be honest, not gentle.",
    inputPrompt: "message code-reviewer",
  },
  {
    wire: "rubber-duck",
    shortLabel: "Duck",
    fullLabel: "the duck",
    emptyHeading: "The duck is listening.",
    emptyQuote: "What did you expect to happen?",
    inputPrompt: "message rubber-duck",
  },
];

export function findPersonality(wire: PersonalityWire): PersonalityMeta {
  const meta = PERSONALITIES.find((p) => p.wire === wire);
  if (!meta) {
    throw new Error(`Unknown personality: ${wire}`);
  }
  return meta;
}

export const DEFAULT_PERSONALITY: PersonalityWire = "senior-architect";
