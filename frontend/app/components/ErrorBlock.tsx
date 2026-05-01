type Props = {
  personalityFullLabel: string;
  onRetry: () => void;
};

export function ErrorBlock({ personalityFullLabel, onRetry }: Props) {
  return (
    <div
      role="alert"
      aria-live="assertive"
      className="bg-error-bg border border-error-border border-l-[3px] border-l-error-accent px-5 py-4 rounded mb-5"
    >
      <div className="text-[9px] uppercase tracking-[0.16em] text-error-accent font-medium mb-1.5">
        {personalityFullLabel} · unreachable
      </div>
      <p className="text-[13px] text-text-emphasis leading-[1.5] mb-2.5">
        The mentor is offline right now. Your message wasn&rsquo;t lost — try again
        in a moment.
      </p>
      <button
        onClick={onRetry}
        className="bg-text-primary text-cream border-none px-3.5 py-1.5 text-[10px] uppercase tracking-[0.1em] cursor-pointer font-sans focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-error-accent focus-visible:ring-offset-2 focus-visible:ring-offset-cream"
      >
        Retry
      </button>
    </div>
  );
}
