import type { ReactNode } from "react";

export function ReadingColumn({ children }: { children: ReactNode }) {
  return (
    <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-8 sm:py-9">
      <div className="max-w-[580px] mx-auto">{children}</div>
    </main>
  );
}
