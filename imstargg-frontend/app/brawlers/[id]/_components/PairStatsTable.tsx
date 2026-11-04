"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Handshake } from "lucide-react";
import type { PairResultStat } from "@/domain/statistics/types";
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
  items: PairResultStat[];
  initialSort?: ComboStatsSortBy;
  initialOrder?: SortOrder;
};

function pairRail(winRate: number): "win" | "neutral" | "loss" {
  if (winRate >= 0.55) return "win";
  if (winRate >= 0.5)  return "neutral";
  return "loss";
}

export function PairStatsTable({
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

  const sorted = useMemo(
    () => sortComboStats(items, sort, order),
    [items, sort, order],
  );

  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <Handshake size={16} aria-hidden className="text-[#2BD576]" />
          조합
        </h3>
        <span className="text-[11px] text-[#5C6271]">함께 픽됐을 때의 승률 · 전투 30회 이상 조합만 보입니다</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_32px_44px_52px_52px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>조합 브롤러</span>
        <SortableHeader column="TIER_SCORE" label={COMBO_SORT_LABELS.TIER_SCORE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="PICK_RATE" label={COMBO_SORT_LABELS.PICK_RATE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="WIN_RATE" label={COMBO_SORT_LABELS.WIN_RATE} currentSort={sort} currentOrder={order} onSort={onSort} />
        <SortableHeader column="BATTLE_COUNT" label={COMBO_SORT_LABELS.BATTLE_COUNT} currentSort={sort} currentOrder={order} onSort={onSort} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        sorted.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              data-rail={pairRail(it.winRate)}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_32px_44px_52px_52px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_88px_96px] sm:gap-2 sm:text-sm"
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
              <TierBadge tier={it.tier} />
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
