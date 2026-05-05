"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";
import StarIcon from "@/components/primitives/StarIcon";
import { cn } from "@/lib/cn";

type Props = {
  items: BrawlerResultStat[];
  sort: string;
  order: SortOrder;
  selectedBrawlerId: number | null;
};

const resultGrid =
  "grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px]";

export function EventResultStatsTable({
  items,
  sort,
  order,
  selectedBrawlerId,
}: Props) {
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
      <div
        className={cn(
          "grid items-center gap-1 border-b border-[#D8DCE5] bg-[#F6F7FA] px-2 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:gap-2 sm:px-3",
          resultGrid,
        )}
      >
        <span>#</span>
        <span>브롤러</span>
        <SortableHeader
          column="tier"
          label="티어"
          currentSort={sort}
          currentOrder={order}
        />
        <SortableHeader
          column="winRate"
          label="승률"
          currentSort={sort}
          currentOrder={order}
        />
        <span>
          <SortableHeader
            column="pickRate"
            label="픽률"
            currentSort={sort}
            currentOrder={order}
          />
        </span>
        <span>
          <SortableHeader
            column="starPlayerRate"
            label={<StarIcon />}
            title="스타플레이어 비율"
            currentSort={sort}
            currentOrder={order}
          />
        </span>
        <span>
          <SortableHeader
            column="totalBattleCount"
            label="전투수"
            currentSort={sort}
            currentOrder={order}
          />
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
              aria-pressed={isActive}
              onClick={() => id > 0 && toggleBrawler(id)}
              className={cn(
                "grid w-full items-center gap-1 border-b border-[#ECEEF3] px-2 py-2 text-left text-[11px] transition-colors hover:bg-[#F6F7FA] sm:gap-2 sm:px-3 sm:text-sm",
                resultGrid,
                isActive && "border-l-4 border-l-[#FFC60A] bg-[#FFE082]/40",
              )}
            >
              <span className="font-bold text-[#5C6271] tabular-nums">
                {i + 1}
              </span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                {it.brawler && (
                  <BrawlerAvatar
                    name={it.brawler.name}
                    imageUrl={it.brawler.portraitImageUrl}
                    rarity={it.brawler.rarity}
                    size={32}
                    className="shrink-0"
                  />
                )}
                <span className="truncate font-bold">
                  {it.brawler?.name ?? "-"}
                </span>
              </div>
              <TierBadge tier={it.tier} />
              <PercentCell value={it.winRate} />
              <span>
                <PercentCell value={it.pickRate} />
              </span>
              <span>
                <PercentCell value={it.starPlayerRate} />
              </span>
              <span>
                <CountCell value={it.totalBattleCount} />
              </span>
            </button>
          );
        })
      )}
    </div>
  );
}
