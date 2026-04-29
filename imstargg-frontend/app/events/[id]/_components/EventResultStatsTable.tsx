"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: BrawlerResultStat[];
  sort: string;
  order: SortOrder;
  selectedBrawlerId: number | null;
};

export function EventResultStatsTable({ items, sort, order, selectedBrawlerId }: Props) {
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
      <div className="grid grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px]">
        <span>#</span>
        <span>브롤러</span>
        <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} />
        <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} />
        <span className="hidden sm:inline">
          <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} />
        </span>
        <span className="hidden sm:inline">
          <SortableHeader column="starPlayerRate" label="MVP" currentSort={sort} currentOrder={order} />
        </span>
        <span className="hidden sm:inline">
          <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
        </span>
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
              className={`grid w-full grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-left transition-colors hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px] ${
                isActive ? "bg-[#FFE082]/40 border-l-4 border-l-[#FFC60A]" : ""
              }`}
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.brawler?.portraitImageUrl && (
                  <Image
                    src={it.brawler.portraitImageUrl}
                    alt={it.brawler.name}
                    width={32}
                    height={32}
                    unoptimized
                    className="h-8 w-8 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.brawler?.name ?? "-"}</span>
              </div>
              <TierBadge tier={it.tier} />
              <PercentCell value={it.winRate} />
              <span className="hidden sm:inline">
                <PercentCell value={it.pickRate} />
              </span>
              <span className="hidden sm:inline">
                <PercentCell value={it.starPlayerRate} />
              </span>
              <span className="hidden sm:inline">
                <CountCell value={it.totalBattleCount} />
              </span>
            </button>
          );
        })
      )}
    </div>
  );
}
