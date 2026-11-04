"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { ComboRankingStat } from "@/domain/statistics/types";
import type { StatTier } from "@/domain/statistics/tier";
import {
  COMBO_SORT_LABELS,
  type ComboStatsSortBy,
  type SortOrder,
  sortComboStats,
  toggleSortOrder,
} from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: ComboRankingStat[];
  initialSort?: ComboStatsSortBy;
  initialOrder?: SortOrder;
};

function railFor(rank: number, tier: StatTier): string {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  switch (tier) {
    case "S": return "win";
    case "A":
    case "B": return "neutral";
    case "C":
    case "D":
    default:  return "loss";
  }
}

const GRID =
  "grid-cols-[24px_minmax(0,1fr)_32px_42px_42px_46px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_96px]";

export function ComboRankingTable({
  items,
  initialSort = "TIER_SCORE",
  initialOrder = "DESC",
}: Props) {
  const [sort, setSort] = useState<ComboStatsSortBy>(initialSort);
  const [order, setOrder] = useState<SortOrder>(initialOrder);

  function onSort(column: ComboStatsSortBy) {
    if (column === sort) {
      setOrder(toggleSortOrder(order));
    } else {
      setSort(column);
      setOrder("DESC");
    }
  }

  // 순위가 비지 않도록 정렬 전에 2~3인이 아닌 행을 걸러낸다
  const sorted = useMemo(
    () =>
      sortComboStats(
        items.filter((it) => it.brawlers.length === 2 || it.brawlers.length === 3),
        sort,
        order,
      ),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className={`data-table-head grid ${GRID} items-center gap-1 sm:gap-2`}>
        <span>순위</span>
        <span>조합 브롤러</span>
        <SortableHeader column="TIER_SCORE" label={COMBO_SORT_LABELS.TIER_SCORE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="WIN_RATE" label={COMBO_SORT_LABELS.WIN_RATE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="PICK_RATE" label={COMBO_SORT_LABELS.PICK_RATE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="BATTLE_COUNT" label={COMBO_SORT_LABELS.BATTLE_COUNT} currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          전투 100회 이상인 조합이 없습니다
        </div>
      ) : (
        sorted.map((it, i) => (
          <div
            key={`${it.brawlers.map((b) => b.id).join("-")}-${i}`}
            data-rail={railFor(i + 1, it.tier)}
            className={`data-row grid ${GRID} items-center gap-1 text-[13px] sm:gap-2 sm:text-sm`}
          >
            <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
            <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
              {it.brawlers.map((b) => (
                <Link
                  key={b.id}
                  href={`/brawlers/${b.id}`}
                  aria-label={b.name}
                  className="shrink-0"
                >
                  <BrawlerAvatar
                    name={b.name}
                    imageUrl={b.portraitImageUrl}
                    rarity={b.rarity}
                    size={32}
                  />
                </Link>
              ))}
            </div>
            <TierBadge tier={it.tier} />
            <PercentCell value={it.winRate} />
            <PercentCell value={it.pickRate} />
            <CountCell value={it.battleCount} />
          </div>
        ))
      )}
    </div>
  );
}
