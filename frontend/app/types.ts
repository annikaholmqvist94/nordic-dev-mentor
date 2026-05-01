export type PersonalityWire =
  | "junior-helper"
  | "senior-architect"
  | "code-reviewer"
  | "rubber-duck";

export type ConversationEntry =
  | { id: string; kind: "user"; content: string }
  | { id: string; kind: "assistant"; content: string; personality: PersonalityWire }
  | { id: string; kind: "switch"; from: PersonalityWire; to: PersonalityWire };

export type SendMessageResult =
  | { ok: true; sessionId: string; reply: string }
  | { ok: false; error: string };
