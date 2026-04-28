type Props = {
  rank: number;
  rankChange: number | null;
};

export function RankBadge({ rank, rankChange }: Props) {
  return (
    <div className="flex items-center gap-1">
      <span className="text-sm font-bold tabular-nums">#{rank}</span>
      {rankChange !== null && <RankChangeIndicator change={rankChange} />}
    </div>
  );
}

function RankChangeIndicator({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="text-xs tabular-nums text-emerald-600">▲{change}</span>
    );
  }
  if (change < 0) {
    return (
      <span className="text-xs tabular-nums text-red-600">▼{Math.abs(change)}</span>
    );
  }
  return <span className="text-xs text-muted-foreground">─</span>;
}
