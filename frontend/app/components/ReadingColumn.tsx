import type { ReactNode } from "react";

export function ReadingColumn({ children }: { children: ReactNode }) {
  return (
    <main className="flex-1 overflow-y-auto px-8 py-9">
      <div className="max-w-[580px] mx-auto">{children}</div>
    </main>
  );
}
