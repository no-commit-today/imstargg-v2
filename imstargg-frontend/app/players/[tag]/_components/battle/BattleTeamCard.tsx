import { BattlePlayerRow } from "./BattlePlayerRow";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  team: BattlePlayer[];
  label: string;             // "WIN" / "LOSS" / "DRAW" / "#1" 등
  labelColor: "green" | "red" | "neutral" | "yellow";
  ownerTag: string;
  starPlayerTag: string | null;
  containsOwner: boolean;
}

const COLOR_MAP: Record<"green" | "red" | "neutral" | "yellow", string> = {
  green: "#2BD576",
  red: "#FF3A3A",
  neutral: "#8A90A0",
  yellow: "#FFC60A",
};

export function BattleTeamCard({
  team, label, labelColor, ownerTag, starPlayerTag, containsOwner,
}: Props) {
  return (
    <div
      style={{
        border: containsOwner
          ? "2px solid rgba(255,198,10,0.4)"
          : "1px solid #D8DCE5",
        borderRadius: 8,
        background: "#fff",
        padding: 8,
      }}
    >
      <div
        style={{
          fontSize: 11,
          fontWeight: 700,
          letterSpacing: "0.04em",
          color: COLOR_MAP[labelColor],
          marginBottom: 4,
          textTransform: "uppercase",
        }}
      >
        {label}
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        {team.map((p) => (
          <BattlePlayerRow
            key={p.tag}
            player={p}
            ownerTag={ownerTag}
            starPlayerTag={starPlayerTag}
          />
        ))}
      </div>
    </div>
  );
}
