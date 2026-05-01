import Link from "next/link";
import type { EnemyResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: EnemyResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

export function EnemyStatsTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">카운터</h3>
      </div>
      <div className="grid grid-cols-[28px_minmax(0,1fr)_44px_44px_56px] items-center gap-1 border-b border-[#D8DCE5] px-2 py-2 text-[10px] font-bold tracking-wider text-[#5C6271] sm:grid-cols-[48px_minmax(0,1fr)_72px_72px_96px] sm:gap-2 sm:px-3 sm:text-[11px]">
        <span>순위</span>
        <span>카운터 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <span>전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.enemyBrawler ? (
            <Link
              key={`${it.enemyBrawler.id}-${i}`}
              href={`/brawlers/${it.enemyBrawler.id}`}
              className="grid grid-cols-[28px_minmax(0,1fr)_44px_44px_56px] items-center gap-1 border-b border-[#ECEEF3] px-2 py-2 text-[11px] hover:bg-[#F6F7FA] sm:grid-cols-[48px_minmax(0,1fr)_72px_72px_96px] sm:gap-2 sm:px-3 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.enemyBrawler.name}
                  imageUrl={it.enemyBrawler.portraitImageUrl}
                  rarity={it.enemyBrawler.rarity}
                  size={28}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.enemyBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
