import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import PowerPill from "@/components/primitives/PowerPill";
import { BattleResultBadge } from "./BattleResultBadge";
import { BattleModeIcon } from "./BattleModeIcon";
import { modeLabel } from "@/domain/battle/modeLabels";
import { formatRelativeTime } from "@/lib/relativeTime";
import { isSameTag } from "@/lib/tag";
import type { Battle, BattlePlayer, BattleType } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
  expanded: boolean;
}

const TYPE_LABELS: Partial<Record<BattleType, string>> = {
  SOLO_RANKED: "솔로 랭크",
  FRIENDLY: "친선",
  CHALLENGE: "챌린지",
  TOURNAMENT: "토너먼트",
  CHAMPIONSHIP_CHALLENGE: "챔피언십",
};

function findOwnerPlayer(battle: Battle, ownerTag: string): BattlePlayer | null {
  for (const team of battle.teams) {
    for (const p of team) {
      if (isSameTag(p.tag, ownerTag)) return p;
    }
  }
  return null;
}

function sideBarColor(battle: Battle): string {
  if (battle.resultType === "RANK" && battle.rank != null) {
    const top = battle.rank <= Math.ceil(battle.teams.length / 3);
    return top ? "#2BD576" : "#8A90A0";
  }
  if (battle.result === "VICTORY") return "#2BD576";
  if (battle.result === "DEFEAT") return "#FF3A3A";
  return "#8A90A0";
}

function trophyChangeColor(n: number): string {
  if (n > 0) return "#2BD576";
  if (n < 0) return "#FF3A3A";
  return "#8A90A0";
}

export function BattleRowHeader({ battle, ownerTag, expanded }: Props) {
  const owner = findOwnerPlayer(battle, ownerTag);
  const ownerBrawler = owner?.brawler.brawler ?? null;
  const modeText = battle.event?.modeName ?? modeLabel(battle.mode);
  const mapText = battle.event?.mapName ?? "알 수 없는 맵";
  const typeLabel = battle.type != null ? TYPE_LABELS[battle.type] : undefined;

  return (
    <div
      className="battle-row-header"
      style={{
        display: "grid",
        gridTemplateColumns: "4px 28px 1fr auto auto auto 24px",
        alignItems: "center",
        gap: 12,
        padding: "10px 12px",
      }}
    >
      <div style={{ width: 4, height: 40, background: sideBarColor(battle), borderRadius: 2 }} />
      <BattleModeIcon iconUrl={battle.event?.modeIconUrl ?? null} mode={battle.mode} size={28} />
      <div style={{ minWidth: 0 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <span style={{ fontSize: 14, fontWeight: 700, color: "#14161B" }}>{modeText}</span>
          {typeLabel && (
            <span
              className="pill"
              style={{
                fontSize: 10,
                padding: "2px 6px",
                background: "#ECEEF3",
                color: "#5C6271",
              }}
            >
              {typeLabel}
            </span>
          )}
        </div>
        <div style={{ fontSize: 12, color: "#5C6271" }}>{mapText}</div>
      </div>
      {owner && (
        <div className="battle-row-owner-brawler" style={{ display: "flex", gap: 4, alignItems: "center" }}>
          <BrawlerAvatar
            name={ownerBrawler?.name ?? "?"}
            imageUrl={ownerBrawler?.portraitImageUrl ?? null}
            size={28}
          />
          <PowerPill level={owner.brawler.power} />
        </div>
      )}
      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
        <BattleResultBadge battle={battle} totalTeams={battle.teams.length} />
        {battle.trophyChange != null && battle.trophyChange !== 0 && (
          <span
            style={{
              fontFamily: "var(--font-num)",
              fontWeight: 700,
              fontSize: 13,
              color: trophyChangeColor(battle.trophyChange),
            }}
          >
            {battle.trophyChange > 0 ? "+" : ""}{battle.trophyChange}
          </span>
        )}
      </div>
      <div className="battle-row-relative-time" style={{ fontSize: 12, color: "#5C6271", whiteSpace: "nowrap" }}>
        {formatRelativeTime(battle.battleTime)}
      </div>
      <div
        aria-hidden
        style={{
          transform: expanded ? "rotate(180deg)" : "rotate(0deg)",
          transition: "transform 150ms cubic-bezier(0.34,1.56,0.64,1)",
          color: "#8A90A0",
          fontSize: 14,
        }}
      >
        ▾
      </div>
    </div>
  );
}
