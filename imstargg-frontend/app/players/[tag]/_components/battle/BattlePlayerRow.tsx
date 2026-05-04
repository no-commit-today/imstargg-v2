import Link from "next/link";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import PowerLevel from "@/components/primitives/PowerLevel";
import RankTierIcon from "@/components/primitives/RankTierIcon";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";
import StarIcon from "@/components/primitives/StarIcon";
import { isSameTag, toUrlPath } from "@/lib/tag";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  player: BattlePlayer;
  ownerTag: string;
  starPlayerTag: string | null;
}

export function BattlePlayerRow({ player, ownerTag, starPlayerTag }: Props) {
  const isOwner = isSameTag(player.tag, ownerTag);
  const isStar = starPlayerTag != null && isSameTag(player.tag, starPlayerTag);
  const playerBrawler = player.brawler.brawler;

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
      <BrawlerAvatar
        name={playerBrawler?.name ?? "?"}
        imageUrl={playerBrawler?.portraitImageUrl ?? null}
        rarity={playerBrawler?.rarity}
        size={32}
      />
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
          {isStar && <StarIcon size={12} className="ml-1 inline-block align-middle" />}
        </Link>
        <div style={{ fontSize: 11, color: "#8A90A0", fontFamily: "var(--font-mono)" }}>
          {player.tag}
        </div>
      </div>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <PowerLevel level={player.brawler.power} size={18} />
        {player.brawler.trophies != null && <BrawlerTrophy value={player.brawler.trophies} size={12} />}
        {player.soloRankTier != null && <RankTierIcon tier={player.soloRankTier} size={16} />}
      </div>
    </div>
  );
}
