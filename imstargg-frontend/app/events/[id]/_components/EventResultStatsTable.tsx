"use client";

import { useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { toggleSortOrder } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";
import StarIcon from "@/components/primitives/StarIcon";

type Props = {
  items: BrawlerResultStat[];
  selectedBrawlerId: number | null;
};

type SortKey =
  | "tier"
  | "winRate"
  | "pickRate"
  | "starPlayerRate"
  | "totalBattleCount";

const resultGrid =
  "grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px]";

function rowRail(rank: number, tier: string): "gold" | "silver" | "bronze" | "win" | "neutral" | "loss" {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  if (tier === "S") return "win";
  if (tier === "A" || tier === "B") return "neutral";
  return "loss";
}

function compareItems(a: BrawlerResultStat, b: BrawlerResultStat, sort: SortKey, order: SortOrder): number {
  const sign = order === "DESC" ? -1 : 1;
  switch (sort) {
    case "winRate": return (a.winRate - b.winRate) * sign;
    case "pickRate": return (a.pickRate - b.pickRate) * sign;
    case "starPlayerRate": return (a.starPlayerRate - b.starPlayerRate) * sign;
    case "totalBattleCount": return (a.totalBattleCount - b.totalBattleCount) * sign;
    case "tier":
    default: return (a.tierScore - b.tierScore) * sign;
  }
}

export function EventResultStatsTable({ items, selectedBrawlerId }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const [sort, setSort] = useState<SortKey>("tier");
  const [order, setOrder] = useState<SortOrder>("DESC");

  function onSort(column: SortKey) {
    if (column === sort) {
      setOrder(toggleSortOrder(order));
    } else {
      setSort(column);
      setOrder("DESC");
    }
  }

  function toggleBrawler(id: number) {
    const sp = new URLSearchParams(params);
    if (selectedBrawlerId === id) sp.delete("brawlerId");
    else sp.set("brawlerId", String(id));
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  const sorted = useMemo(
    () => [...items].sort((a, b) => compareItems(a, b, sort, order)),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className={`data-table-head grid items-center gap-1 sm:gap-2 ${resultGrid}`}>
        <span>순위</span>
        <span>브롤러</span>
        <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="starPlayerRate" label={<StarIcon />} title="스타플레이어 비율" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        sorted.map((it, i) => {
          const id = it.brawler?.id ?? -1;
          const isActive = selectedBrawlerId === id;
          return (
            <button
              key={`${id}-${i}`}
              type="button"
              aria-pressed={isActive}
              onClick={() => id > 0 && toggleBrawler(id)}
              data-rail={rowRail(i + 1, it.tier)}
              data-selected={isActive ? "true" : undefined}
              className={`data-row grid w-full items-center gap-1 text-left text-[13px] sm:gap-2 sm:text-sm ${resultGrid}`}
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
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
                <span className="truncate font-bold">{it.brawler?.name ?? "-"}</span>
              </div>
              <TierBadge tier={it.tier} />
              <PercentCell value={it.winRate} />
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.starPlayerRate} />
              <CountCell value={it.totalBattleCount} />
            </button>
          );
        })
      )}
    </div>
  );
}
