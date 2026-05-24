type Medal = "gold" | "silver" | "bronze";

function medalFor(rank: number): Medal | null {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  return null;
}

export function RankNumber({ rank }: { rank: number }) {
  const medal = medalFor(rank);
  if (medal) {
    return (
      <span className="rank-medal" data-medal={medal} aria-label={`${rank}위`}>
        {rank}
      </span>
    );
  }
  return (
    <span className="rank-number" aria-label={`${rank}위`}>
      {rank}
    </span>
  );
}
