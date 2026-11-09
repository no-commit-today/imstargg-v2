"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { compareBrawlerTier, toggleSortOrder } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import { UngradedTierDescription } from "@app/_components/statistics/UngradedTierDescription";
import type { StatTier } from "@/domain/statistics/tier";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";
import StarIcon from "@/components/primitives/StarIcon";

type Props = {
  items: BrawlerResultStat[];
};

type SortKey =
  | "tier"
  | "winRate"
  | "pickRate"
  | "starPlayerRate"
  | "battleCount";

const resultGrid =
  "grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px]";

function rowRail(rank: number, tier: StatTier | null): "gold" | "silver" | "bronze" | "win" | "neutral" | "loss" {
  if (tier === null) return "neutral";
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
    case "battleCount": return (a.battleCount - b.battleCount) * sign;
    case "tier":
    default: return compareBrawlerTier(a, b, order);
  }
}

export function EventResultStatsTable({ items }: Props) {
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
        <SortableHeader column="battleCount" label="전투수" currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        sorted.map((it, i) => {
          const rowClassName = `data-row grid items-center gap-1 text-[13px] sm:gap-2 sm:text-sm ${resultGrid}`;
          const rail = rowRail(i + 1, it.tier);
          const cells = (
            <>
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
              <CountCell value={it.battleCount} />
            </>
          );
          return it.brawler ? (
            <Link
              key={`${it.brawler.id}-${i}`}
              href={`/brawlers/${it.brawler.id}`}
              data-rail={rail}
              className={rowClassName}
            >
              {cells}
            </Link>
          ) : (
            <div key={`-1-${i}`} data-rail={rail} className={rowClassName}>
              {cells}
            </div>
          );
        })
      )}
      {sorted.some((it) => it.tier === null) && <UngradedTierDescription />}
    </div>
  );
}
