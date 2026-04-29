import Image from "next/image";
import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  rank: number;
  brawler: Brawler;
  stat: BrawlerResultStat | null;
};

export function BrawlerStatRow({ rank, brawler, stat }: Props) {
  return (
    <Link
      href={`/brawlers/${brawler.id}`}
      className="grid grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px]"
    >
      <span className="text-sm font-bold text-[#5C6271] tabular-nums">{rank}</span>
      <div className="flex items-center gap-2 overflow-hidden">
        {brawler.portraitImageUrl && (
          <Image
            src={brawler.portraitImageUrl}
            alt={brawler.name}
            width={32}
            height={32}
            unoptimized
            className="h-8 w-8 rounded-md"
          />
        )}
        <span className="truncate text-sm font-bold">{brawler.name}</span>
      </div>
      <div>{stat ? <TierBadge tier={stat.tier} /> : <TierBadge tier="D" />}</div>
      <PercentCell value={stat?.winRate ?? 0} />
      <span className="hidden sm:inline">
        <PercentCell value={stat?.pickRate ?? 0} />
      </span>
      <span className="hidden sm:inline">
        <PercentCell value={stat?.starPlayerRate ?? 0} />
      </span>
      <span className="hidden sm:inline">
        <CountCell value={stat?.totalBattleCount ?? 0} />
      </span>
    </Link>
  );
}
