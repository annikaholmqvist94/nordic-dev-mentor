export function TopBar() {
  return (
    <header className="h-14 border-b border-border-subtle flex items-center justify-between px-8 shrink-0">
      <div className="font-serif text-lg font-medium tracking-tight">
        Nordic Dev Mentor<span className="text-accent">.</span>
      </div>
      <nav className="flex gap-6 text-[11px] uppercase tracking-[0.1em] text-text-muted">
        <a className="text-text-primary border-b border-accent pb-1.5" href="/">
          Chat
        </a>
        <a className="cursor-pointer hover:text-text-primary" href="/history">
          History
        </a>
        <a className="cursor-pointer hover:text-text-primary" href="/about">
          About
        </a>
      </nav>
    </header>
  );
}
