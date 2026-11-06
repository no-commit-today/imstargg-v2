"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { BrawlerRankStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { toggleSortOrder } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: BrawlerRankStat[];
};

type SortKey = "averageRank" | "totalBattleCount";

const rankGrid =
  "grid-cols-[26px_minmax(0,1fr)_64px_52px] sm:grid-cols-[48px_minmax(0,1fr)_88px_96px]";

function rowRail(rank: number): "gold" | "silver" | "bronze" | undefined {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  return undefined;
}

function compareItems(a: BrawlerRankStat, b: BrawlerRankStat, sort: SortKey, order: SortOrder): number {
  const sign = order === "DESC" ? -1 : 1;
  switch (sort) {
    case "totalBattleCount":
      return (a.totalBattleCount - b.totalBattleCount) * sign;
    case "averageRank":
    default:
      return (a.averageRank - b.averageRank) * sign;
  }
}

export function EventRankStatsTable({ items }: Props) {
  const [sort, setSort] = useState<SortKey>("averageRank");
  const [order, setOrder] = useState<SortOrder>("ASC");

  function onSort(column: SortKey) {
    if (column === sort) {
      setOrder(toggleSortOrder(order));
    } else {
      setSort(column);
      setOrder(column === "averageRank" ? "ASC" : "DESC");
    }
  }

  const sorted = useMemo(
    () => [...items].sort((a, b) => compareItems(a, b, sort, order)),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className={`data-table-head grid items-center gap-1 sm:gap-2 ${rankGrid}`}>
        <span>순위</span>
        <span>브롤러</span>
        <SortableHeader column="averageRank" label="평균순위" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        sorted.map((it, i) => {
          const rowClassName = `data-row grid items-center gap-1 text-[13px] sm:gap-2 sm:text-sm ${rankGrid}`;
          const rail = rowRail(i + 1);
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
              <AverageRankCell value={it.averageRank} />
              <CountCell value={it.totalBattleCount} />
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
    </div>
  );
}
