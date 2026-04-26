interface Props {
  result: "win" | "loss" | "draw" | null;
  rank?: number | null;
}

export default function ResultPill({ result, rank }: Props) {
  if (rank != null) {
    const top = rank <= 4;
    return (
      <span
        className={`pill ${top ? "green" : rank <= 7 ? "" : "red"}`}
        style={{ fontFamily: "var(--font-num)" }}
      >
        #{rank}
      </span>
    );
  }
  if (result === "win") return <span className="pill green">WIN</span>;
  if (result === "loss") return <span className="pill red">LOSS</span>;
  return <span className="pill">DRAW</span>;
}
