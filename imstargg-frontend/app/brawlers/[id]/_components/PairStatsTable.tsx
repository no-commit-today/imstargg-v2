import Image from "next/image";
import Link from "next/link";
import type { PairResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

export function PairStatsTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">페어 통계</h3>
      </div>
      <div className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]">
        <span>#</span>
        <span>페어 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <span className="hidden sm:inline">전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]"
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.pairBrawler.portraitImageUrl && (
                  <Image
                    src={it.pairBrawler.portraitImageUrl}
                    alt={it.pairBrawler.name}
                    width={28}
                    height={28}
                    unoptimized
                    className="h-7 w-7 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.pairBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} />
              <span className="hidden sm:inline">
                <CountCell value={it.totalBattleCount} />
              </span>
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
