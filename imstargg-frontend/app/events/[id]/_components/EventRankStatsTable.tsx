"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerRankStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: BrawlerRankStat[];
  sort: string;
  order: SortOrder;
  selectedBrawlerId: number | null;
};

export function EventRankStatsTable({ items, sort, order, selectedBrawlerId }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function toggleBrawler(id: number) {
    const sp = new URLSearchParams(params);
    if (selectedBrawlerId === id) sp.delete("brawlerId");
    else sp.set("brawlerId", String(id));
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>브롤러</span>
        <SortableHeader column="averageRank" label="평균순위" currentSort={sort} currentOrder={order} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        items.map((it, i) => {
          const id = it.brawler?.id ?? -1;
          const isActive = selectedBrawlerId === id;
          return (
            <button
              key={`${id}-${i}`}
              type="button"
              onClick={() => id > 0 && toggleBrawler(id)}
              className={`grid w-full grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-left hover:bg-[#F6F7FA] ${
                isActive ? "bg-[#FFE082]/40 border-l-4 border-l-[#FFC60A]" : ""
              }`}
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.brawler && (
                  <BrawlerAvatar
                    name={it.brawler.name}
                    imageUrl={it.brawler.portraitImageUrl}
                    rarity={it.brawler.rarity}
                    size={32}
                    className="shrink-0"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.brawler?.name ?? "-"}</span>
              </div>
              <AverageRankCell value={it.averageRank} />
              <CountCell value={it.totalBattleCount} />
            </button>
          );
        })
      )}
    </div>
  );
}
