export function TopBar() {
  return (
    <header className="h-14 border-b border-border-subtle flex items-center justify-between px-4 sm:px-8 shrink-0 gap-3">
      <div className="font-serif text-base sm:text-lg font-medium tracking-tight shrink-0">
        Nordic Dev Mentor<span className="text-accent">.</span>
      </div>
      <nav className="flex gap-4 sm:gap-6 text-[10px] sm:text-[11px] uppercase tracking-[0.1em] text-text-muted">
        <a className="text-text-primary border-b border-accent pb-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-cream rounded-sm" href="/">
          Chat
        </a>
        <a className="cursor-pointer hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-cream rounded-sm" href="/history">
          History
        </a>
        <a className="cursor-pointer hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-cream rounded-sm" href="/about">
          About
        </a>
      </nav>
    </header>
  );
}
