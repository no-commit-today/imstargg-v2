import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import ClassIcon from "@/components/primitives/ClassIcon";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";
import type { StatTier } from "@/domain/statistics/tier";

type Props = {
  rank: number;
  brawler: Brawler;
  stat: BrawlerResultStat | null;
};

function railFor(rank: number, tier: StatTier | undefined): string {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  switch (tier) {
    case "S": return "win";
    case "A":
    case "B": return "neutral";
    case "C":
    case "D":
    default:  return "loss";
  }
}

export function BrawlerStatRow({ rank, brawler, stat }: Props) {
  const rail = railFor(rank, stat?.tier);
  return (
    <Link
      href={`/brawlers/${brawler.id}`}
      data-rail={rail}
      className="data-row grid grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px] sm:gap-2 sm:text-sm"
    >
      <span className="font-bold text-[#5C6271] tabular-nums">{rank}</span>
      <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
        <BrawlerAvatar
          name={brawler.name}
          imageUrl={brawler.portraitImageUrl}
          rarity={brawler.rarity}
          size={36}
          className="shrink-0"
        />
        <span className="shrink-0">
          <ClassIcon role={brawler.role} size={14} />
        </span>
        <span className="truncate font-bold">{brawler.name}</span>
      </div>
      <div>{stat ? <TierBadge tier={stat.tier} /> : <TierBadge tier="D" />}</div>
      <PercentCell value={stat?.winRate ?? 0} />
      <PercentCell value={stat?.pickRate ?? 0} />
      <PercentCell value={stat?.starPlayerRate ?? 0} />
      <CountCell value={stat?.totalBattleCount ?? 0} />
    </Link>
  );
}
