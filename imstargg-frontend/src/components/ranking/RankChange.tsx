type Props = {
  change: number | null;
};

export function RankChange({ change }: Props) {
  if (change === null) return null;
  if (change > 0) {
    return (
      <span className="text-[11px] font-bold tabular-nums text-[var(--color-accent-green)]">
        ▲{change}
      </span>
    );
  }
  if (change < 0) {
    return (
      <span className="text-[11px] font-bold tabular-nums text-[var(--color-accent-red)]">
        ▼{Math.abs(change)}
      </span>
    );
  }
  return <span className="text-[11px] text-[#8A90A0]">─</span>;
}
