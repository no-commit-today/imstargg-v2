import Link from "next/link";
import type { PairRankStat } from "@/domain/statistics/types";
import type { SortOrder, RankStatsSortBy } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairRankStat[];
  sort: RankStatsSortBy;
  order: SortOrder;
};

export function PairRankTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">조합</h3>
      </div>
      <div className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#D8DCE5] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>조합 브롤러</span>
        <SortableHeader column="AVERAGE_RANK" label="평균순위" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA]"
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                <BrawlerAvatar
                  name={it.pairBrawler.name}
                  imageUrl={it.pairBrawler.portraitImageUrl}
                  rarity={it.pairBrawler.rarity}
                  size={28}
                  className="shrink-0"
                />
                <span className="truncate text-sm font-bold">{it.pairBrawler.name}</span>
              </div>
              <AverageRankCell value={it.averageRank} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
