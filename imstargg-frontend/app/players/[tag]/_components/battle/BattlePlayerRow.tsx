import Link from "next/link";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import PowerPill from "@/components/primitives/PowerPill";
import Trophy from "@/components/primitives/Trophy";
import { isSameTag, toUrlPath } from "@/lib/tag";
import { tierColor, tierLabel } from "@/domain/battle/tierLabels";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  player: BattlePlayer;
  ownerTag: string;
  starPlayerTag: string | null;
}

export function BattlePlayerRow({ player, ownerTag, starPlayerTag }: Props) {
  const isOwner = isSameTag(player.tag, ownerTag);
  const isStar = starPlayerTag != null && isSameTag(player.tag, starPlayerTag);
  const brawlerId = player.brawler.brawler?.id != null
    ? String(player.brawler.brawler.id)
    : "0";

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "32px 1fr auto",
        alignItems: "center",
        gap: 8,
        padding: "6px 8px",
        background: isOwner ? "rgba(255,198,10,0.08)" : "transparent",
        borderRadius: 6,
      }}
    >
      <BrawlerPortrait id={brawlerId} size={32} />
      <div style={{ minWidth: 0 }}>
        <Link
          href={`/players/${toUrlPath(player.tag)}`}
          onClick={(e) => e.stopPropagation()}
          style={{
            display: "block",
            fontWeight: isOwner ? 700 : 500,
            fontSize: 13,
            color: "#14161B",
            textDecoration: "none",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {player.name}
          {isStar && <span style={{ marginLeft: 4, color: "#FFC60A" }}>★</span>}
        </Link>
        <div style={{ fontSize: 11, color: "#8A90A0", fontFamily: "var(--font-mono)" }}>
          {player.tag}
        </div>
      </div>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <PowerPill level={player.brawler.power} />
        {player.brawler.trophies != null && <Trophy value={player.brawler.trophies} size={12} />}
        {player.soloRankTier != null && (
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              background: tierColor(player.soloRankTier),
              color: "#FFFFFF",
              fontFamily: "var(--font-num)",
              fontWeight: 700,
              fontSize: 9,
              padding: "2px 4px",
              borderRadius: 4,
              letterSpacing: "0.02em",
            }}
            title={player.soloRankTier}
          >
            {tierLabel(player.soloRankTier)}
          </span>
        )}
      </div>
    </div>
  );
}
