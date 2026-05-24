"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Handshake } from "lucide-react";
import type { PairRankStat } from "@/domain/statistics/types";
import {
  type RankStatsSortBy,
  type SortOrder,
  sortRankStats,
  toggleSortOrder,
} from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairRankStat[];
  initialSort?: RankStatsSortBy;
  initialOrder?: SortOrder;
};

export function PairRankTable({
  items,
  initialSort = "AVERAGE_RANK",
  initialOrder = "ASC",
}: Props) {
  const [sort, setSort] = useState<RankStatsSortBy>(initialSort);
  const [order, setOrder] = useState<SortOrder>(initialOrder);

  function onSort(column: RankStatsSortBy) {
    if (column === sort) {
      setOrder(toggleSortOrder(order));
    } else {
      setSort(column);
      setOrder(column === "AVERAGE_RANK" ? "ASC" : "DESC");
    }
  }

  const sorted = useMemo(
    () => sortRankStats(items, sort, order),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <Handshake size={16} aria-hidden className="text-[#2BD576]" />
          조합
        </h3>
        <span className="text-[11px] text-[#5C6271]">함께 픽됐을 때의 평균 순위</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_64px_56px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>조합 브롤러</span>
        <SortableHeader column="AVERAGE_RANK" label="평균순위" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="PICK_RATE" label="전투수" currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        sorted.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_64px_56px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_88px_96px] sm:gap-2 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.pairBrawler.name}
                  imageUrl={it.pairBrawler.portraitImageUrl}
                  rarity={it.pairBrawler.rarity}
                  size={32}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.pairBrawler.name}</span>
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
