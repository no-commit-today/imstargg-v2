import type { Battle } from "@/domain/battle/types";

interface Props {
  battle: Pick<Battle, "result" | "rank" | "resultType">;
}

export function BattleResultBadge({ battle }: Props) {
  if (battle.resultType === "RANK" && battle.rank != null) {
    const top = battle.rank <= 3;
    const mid = battle.rank <= 6;
    const cls = top ? "green" : mid ? "" : "red";
    return (
      <span className={`pill ${cls}`} style={{ fontFamily: "var(--font-num)" }}>
        #{battle.rank}
      </span>
    );
  }
  if (battle.result === "VICTORY") return <span className="pill green">WIN</span>;
  if (battle.result === "DEFEAT") return <span className="pill red">LOSS</span>;
  if (battle.result === "DRAW") return <span className="pill">DRAW</span>;
  return <span className="pill">—</span>;
}
