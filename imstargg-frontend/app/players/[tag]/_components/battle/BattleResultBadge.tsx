import type { Battle } from "@/domain/battle/types";

interface Props {
  battle: Pick<Battle, "result" | "rank" | "resultType">;
  totalTeams?: number;
}

export function BattleResultBadge({ battle, totalTeams }: Props) {
  if (battle.resultType === "RANK" && battle.rank != null) {
    const top = battle.rank <= 3;
    const mid = totalTeams != null && battle.rank <= Math.ceil(totalTeams / 2);
    if (top) {
      return (
        <span
          className="pill"
          style={{
            fontFamily: "var(--font-num)",
            background: "#FFC60A",
            color: "#14161B",
          }}
        >
          #{battle.rank}
        </span>
      );
    }
    const cls = mid ? "green" : "red";
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
