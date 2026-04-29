import { BattleTeamCard } from "./BattleTeamCard";
import type { Battle, BattlePlayer } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
}

function teamContainsOwner(team: BattlePlayer[], ownerTag: string): boolean {
  const norm = ownerTag.replace(/^#/, "").toUpperCase();
  return team.some((p) => p.tag.replace(/^#/, "").toUpperCase() === norm);
}

function resultLabel(battle: Battle, teamIndex: number, isOwnerTeam: boolean): {
  label: string;
  color: "green" | "red" | "neutral" | "yellow";
} {
  if (battle.resultType === "RANK") {
    const rank = teamIndex + 1;
    const totalTeams = battle.teams.length;
    const top = rank <= Math.ceil(totalTeams / 3);
    const mid = rank <= Math.ceil(totalTeams * 2 / 3);
    return { label: `#${rank}`, color: top ? "green" : mid ? "neutral" : "red" };
  }
  // RESULT
  if (battle.result === "DRAW") return { label: "DRAW", color: "neutral" };
  const ownerWon = battle.result === "VICTORY";
  const teamWon = isOwnerTeam ? ownerWon : !ownerWon;
  if (teamWon) return { label: "WIN", color: "green" };
  return { label: "LOSS", color: "red" };
}

export function BattleExpandedTeams({ battle, ownerTag }: Props) {
  const numTeams = battle.teams.length;
  const cols = battle.resultType === "RESULT"
    ? "1fr 1fr"
    : numTeams === 10 ? "repeat(2, 1fr)"
    : numTeams === 5 ? "repeat(5, 1fr)"
    : numTeams === 4 ? "repeat(4, 1fr)"
    : `repeat(${numTeams}, 1fr)`;

  return (
    <div
      className="battle-expanded-teams"
      data-result-type={battle.resultType}
      data-team-count={numTeams}
      style={{
        display: "grid",
        gridTemplateColumns: cols,
        gap: 8,
        padding: "8px 12px 12px 16px",
      }}
    >
      {battle.teams.map((team, i) => {
        const isOwner = teamContainsOwner(team, ownerTag);
        const { label, color } = resultLabel(battle, i, isOwner);
        return (
          <BattleTeamCard
            key={i}
            team={team}
            label={label}
            labelColor={color}
            ownerTag={ownerTag}
            starPlayerTag={battle.starPlayerTag}
            containsOwner={isOwner}
          />
        );
      })}
    </div>
  );
}
