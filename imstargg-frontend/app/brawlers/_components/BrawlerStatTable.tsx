"use client";

import { useMemo, useState } from "react";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import { BrawlerStatRow } from "./BrawlerStatRow";
import { BrawlerListHeader } from "./BrawlerListHeader";
import StarIcon from "@/components/primitives/StarIcon";

type Row = { brawler: Brawler; stat: BrawlerResultStat | null };

type Props = {
  rows: Row[];
  sort: string;
  order: SortOrder;
  totalBattleCount: number;
};

function compareRows(a: Row, b: Row, sort: string, order: SortOrder): number {
  const sign = order === "DESC" ? -1 : 1;
  switch (sort) {
    case "name":
      return a.brawler.name.localeCompare(b.brawler.name, "ko") * sign;
    case "winRate":
      return ((a.stat?.winRate ?? 0) - (b.stat?.winRate ?? 0)) * sign;
    case "pickRate":
      return ((a.stat?.pickRate ?? 0) - (b.stat?.pickRate ?? 0)) * sign;
    case "starPlayerRate":
      return ((a.stat?.starPlayerRate ?? 0) - (b.stat?.starPlayerRate ?? 0)) * sign;
    case "totalBattleCount":
      return ((a.stat?.totalBattleCount ?? 0) - (b.stat?.totalBattleCount ?? 0)) * sign;
    case "tier":
    default:
      return ((a.stat?.tierScore ?? 0) - (b.stat?.tierScore ?? 0)) * sign;
  }
}

export function BrawlerStatTable({ rows, sort, order, totalBattleCount }: Props) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => r.brawler.name.toLowerCase().includes(q));
  }, [rows, search]);

  const sorted = useMemo(
    () => [...filtered].sort((a, b) => compareRows(a, b, sort, order)),
    [filtered, sort, order],
  );

  return (
    <div className="data-table">
      <BrawlerListHeader
        search={search}
        onSearchChange={setSearch}
        totalBattleCount={totalBattleCount}
      />
      <div className="data-table-head grid grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px] sm:gap-2">
        <span>순위</span>
        <SortableHeader column="name" label="브롤러" currentSort={sort} currentOrder={order} />
        <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} />
        <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} />
        <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} />
        <SortableHeader column="starPlayerRate" label={<StarIcon />} title="스타플레이어 비율" currentSort={sort} currentOrder={order} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 데이터가 없습니다.
        </div>
      ) : (
        sorted.map((r, i) => (
          <BrawlerStatRow key={r.brawler.id} rank={i + 1} brawler={r.brawler} stat={r.stat} />
        ))
      )}
    </div>
  );
}
