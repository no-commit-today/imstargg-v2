"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Swords } from "lucide-react";
import type { EnemyResultStat } from "@/domain/statistics/types";
import {
  type ResultStatsSortBy,
  type SortOrder,
  sortResultStats,
  toggleSortOrder,
} from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: EnemyResultStat[];
  initialSort?: ResultStatsSortBy;
  initialOrder?: SortOrder;
};

/**
 * 카운터 표의 승률은 "이 상대를 만났을 때 내 승률" 이다.
 * - 내 승률 ≥55%: 내가 유리(win)   — 초록 rail
 * - 내 승률 ≤45%: 내가 불리(loss)  — 빨간 rail
 * - 그 외: neutral
 */
function enemyRail(winRate: number): "win" | "neutral" | "loss" {
  if (winRate >= 0.55) return "win";
  if (winRate <= 0.45) return "loss";
  return "neutral";
}

export function EnemyStatsTable({
  items,
  initialSort = "WIN_RATE",
  initialOrder = "ASC",
}: Props) {
  const [sort, setSort] = useState<ResultStatsSortBy>(initialSort);
  const [order, setOrder] = useState<SortOrder>(initialOrder);

  function onSort(column: ResultStatsSortBy) {
    if (column === sort) {
      setOrder(toggleSortOrder(order));
    } else {
      setSort(column);
      setOrder("DESC");
    }
  }

  const sorted = useMemo(
    () => sortResultStats(items, sort, order),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <Swords size={16} aria-hidden className="text-[#FF3A3A]" />
          카운터
        </h3>
        <span className="text-[11px] text-[#5C6271]">이 상대를 만났을 때 내 승률 · 전투 30회 이상 상대만 보입니다</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>카운터 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} onSort={onSort} />
        <span>전투수</span>
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        sorted.map((it, i) =>
          it.enemyBrawler ? (
            <Link
              key={`${it.enemyBrawler.id}-${i}`}
              href={`/brawlers/${it.enemyBrawler.id}`}
              data-rail={enemyRail(it.winRate)}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.enemyBrawler.name}
                  imageUrl={it.enemyBrawler.portraitImageUrl}
                  rarity={it.enemyBrawler.rarity}
                  size={32}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.enemyBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} />
              <CountCell value={it.battleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
