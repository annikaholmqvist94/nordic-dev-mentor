import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Message } from "../components/Message";

describe("Message", () => {
  it("renders user content as plain text", () => {
    render(<Message kind="user" content="Hello **world**" />);
    expect(screen.getByText("Hello **world**")).toBeInTheDocument();
  });

  it("renders assistant content with markdown bold", () => {
    render(
      <Message
        kind="assistant"
        content="Use **Postgres** for relational queries"
        personality="senior-architect"
      />,
    );
    const strong = screen.getByText("Postgres");
    expect(strong.tagName).toBe("STRONG");
  });

  it("renders assistant code blocks as <pre><code>", () => {
    render(
      <Message
        kind="assistant"
        content={"Try this:\n\n```sql\nSELECT 1\n```"}
        personality="senior-architect"
      />,
    );
    const code = screen.getByText("SELECT 1");
    expect(code.tagName).toBe("CODE");
    expect(code.parentElement?.tagName).toBe("PRE");
  });

  it("renders inline code as <code>", () => {
    render(
      <Message
        kind="assistant"
        content="Use the `jsonb` column type."
        personality="senior-architect"
      />,
    );
    const code = screen.getByText("jsonb");
    expect(code.tagName).toBe("CODE");
    expect(code.parentElement?.tagName).not.toBe("PRE");
  });
});
