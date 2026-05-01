import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { findPersonality } from "../personality";
import type { PersonalityWire } from "../types";

type Props =
  | { kind: "user"; content: string }
  | { kind: "assistant"; content: string; personality: PersonalityWire };

export function Message(props: Props) {
  const isUser = props.kind === "user";
  const roleLabel = isUser
    ? "You"
    : findPersonality(props.personality).shortLabel;

  return (
    <div
      className={`mb-5 pl-4 border-l-2 ${
        isUser ? "border-border-emphasis" : "border-accent"
      }`}
    >
      <div className="text-[9px] uppercase tracking-[0.16em] text-text-muted font-medium mb-1.5">
        {roleLabel}
      </div>
      {isUser ? (
        <div className="text-[14px] leading-[1.65] text-text-emphasis whitespace-pre-wrap">
          {props.content}
        </div>
      ) : (
        <div className="prose-editorial text-[14px] leading-[1.65] text-text-emphasis">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {props.content}
          </ReactMarkdown>
        </div>
      )}
    </div>
  );
}
