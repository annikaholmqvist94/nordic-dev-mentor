import Link from "next/link";
import { TopBar } from "../components/TopBar";
import { PERSONALITIES } from "../personality";

export default function AboutPage() {
  return (
    <div className="flex flex-col min-h-screen">
      <TopBar />
      <main className="flex-1 px-4 py-6 sm:px-8 sm:py-9">
        <div className="max-w-[580px] mx-auto">
          <h1 className="font-serif text-[28px] sm:text-[32px] font-medium leading-[1.2] mb-6">
            About
          </h1>

          <p className="text-[14px] leading-[1.65] text-text-emphasis mb-4">
            Nordic Dev Mentor is a portfolio project — a Spring Boot middleware
            that proxies chat requests to OpenRouter with four distinct mentor
            personalities, paired with a Next.js frontend in editorial Nordic
            design.
          </p>

          <p className="text-[14px] leading-[1.65] text-text-emphasis mb-8">
            Each mentor uses its own sampling temperature so its tone stays
            consistent: the junior helper is patient and pedagogical, the
            architect is judicious, the reviewer is direct, and the rubber duck
            asks Socratic questions instead of giving answers.
          </p>

          <h2 className="font-serif text-[20px] font-medium mb-4">
            Mentors
          </h2>
          <ul className="mb-8">
            {PERSONALITIES.map((p) => (
              <li
                key={p.wire}
                className="border-b border-border-subtle py-3 last:border-b-0"
              >
                <div className="text-[10px] uppercase tracking-[0.12em] text-text-muted mb-1">
                  {p.shortLabel}
                </div>
                <div className="font-serif italic text-[15px] text-accent">
                  &ldquo;{p.emptyQuote}&rdquo;
                </div>
              </li>
            ))}
          </ul>

          <h2 className="font-serif text-[20px] font-medium mb-4">
            Stack
          </h2>
          <ul className="text-[14px] leading-[1.85] text-text-emphasis mb-8 list-disc list-inside marker:text-text-disabled">
            <li>Spring Boot 4 backend on Java 21</li>
            <li>Next.js 16 (App Router) + React 19 + TypeScript + Tailwind 4</li>
            <li>OpenRouter as the LLM provider</li>
            <li>Railway for deployment, internal DNS for backend isolation</li>
            <li>Vitest + React Testing Library + JUnit + WireMock for tests</li>
          </ul>

          <p className="text-[14px] text-text-muted">
            Source on{" "}
            <Link
              href="https://github.com/annikaholmqvist94/nordic-dev-mentor"
              className="text-accent hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-cream rounded-sm"
            >
              GitHub
            </Link>
            . Built by Annika Holmqvist.
          </p>
        </div>
      </main>
    </div>
  );
}
