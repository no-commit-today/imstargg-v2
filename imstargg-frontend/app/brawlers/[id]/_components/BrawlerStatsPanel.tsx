"use client";

import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { applySearchShallow } from "@/lib/shallowSearch";
import {
  DEFAULT_STATS_FILTER,
  useBrawlerStats,
  type BrawlerStatsInitial,
  type StatsFilter,
} from "@/hooks/queries/useBrawlerStats";
import { StatRangeSelectorView } from "@app/_components/statistics/StatRangeSelector";
import { PairStatsTable } from "./PairStatsTable";
import { EnemyStatsTable } from "./EnemyStatsTable";

const DailyTrendChart = dynamic(
  () => import("./DailyTrendChart").then((m) => m.DailyTrendChart),
  {
    ssr: false,
    loading: () => (
      <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">
        차트 불러오는 중…
      </div>
    ),
  },
);

type Props = {
  brawlerId: number;
  initial: BrawlerStatsInitial;
};

export function BrawlerStatsPanel({ brawlerId, initial }: Props) {
  // useSearchParams 대신 useState + popstate 동기화 — 정적 프리렌더에서
  // CSR bailout 없이 기본값 통계가 HTML 에 포함되게 한다.
  const [filter, setFilter] = useState<StatsFilter>(DEFAULT_STATS_FILTER);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setFilter({
        tier: parseTierRange(sp.get("tier") ?? undefined),
        date: parseDateRange(sp.get("date") ?? undefined),
      });
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const { daily, pair, enemy } = useBrawlerStats(brawlerId, filter, initial);

  function handleFilterChange(next: StatsFilter) {
    setFilter(next);
    applySearchShallow({
      tier: next.tier === DEFAULT_TIER_RANGE ? null : next.tier,
      date: next.date === DEFAULT_DATE_RANGE ? null : next.date,
    });
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="section-title">통계</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            {TIER_RANGE_LABELS[filter.tier]} 기준
          </p>
        </div>
        <StatRangeSelectorView
          tier={filter.tier}
          date={filter.date}
          onSelect={handleFilterChange}
        />
      </div>
      <DailyTrendChart items={daily.data ?? []} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.data ?? []} />
        <EnemyStatsTable items={enemy.data ?? []} />
      </div>
    </section>
  );
}
