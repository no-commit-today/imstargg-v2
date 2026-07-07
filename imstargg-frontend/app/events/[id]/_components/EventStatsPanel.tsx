"use client";

import { useEffect, useState } from "react";
import type { BattleEventModeResultType } from "@/api/types";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import type {
  BrawlerRankStat,
  BrawlerResultStatistics,
} from "@/domain/statistics/types";
import {
  useEventRankStats,
  useEventResultStats,
} from "@/hooks/queries/useEventStats";
import {
  DEFAULT_STATS_FILTER,
  type StatsFilter,
} from "@/hooks/queries/useBrawlerStats";
import { applySearchShallow } from "@/lib/shallowSearch";
import { StatRangeSelectorView } from "@app/_components/statistics/StatRangeSelector";
import { EventResultStatsTable } from "./EventResultStatsTable";
import { EventRankStatsTable } from "./EventRankStatsTable";
import { EventSelectedBrawlerPanel } from "./EventSelectedBrawlerPanel";

type Props = {
  eventId: number;
  resultType: BattleEventModeResultType;
  initialResult: BrawlerResultStatistics | null;
  initialRank: BrawlerRankStat[] | null;
};

function parseBrawlerId(raw: string | null): number | null {
  return raw && /^\d+$/.test(raw) ? Number(raw) : null;
}

export function EventStatsPanel({
  eventId,
  resultType,
  initialResult,
  initialRank,
}: Props) {
  const [filter, setFilter] = useState<StatsFilter>(DEFAULT_STATS_FILTER);
  const [selectedBrawlerId, setSelectedBrawlerId] = useState<number | null>(null);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setFilter({
        tier: parseTierRange(sp.get("tier") ?? undefined),
        date: parseDateRange(sp.get("date") ?? undefined),
      });
      setSelectedBrawlerId(parseBrawlerId(sp.get("brawlerId")));
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const isRank = resultType === "RANK";
  const resultStats = useEventResultStats(
    eventId,
    filter,
    initialResult ?? undefined,
    !isRank,
  );
  const rankStats = useEventRankStats(
    eventId,
    filter,
    initialRank ?? undefined,
    isRank,
  );

  function handleFilterChange(next: StatsFilter) {
    setFilter(next);
    applySearchShallow({
      tier: next.tier === DEFAULT_TIER_RANGE ? null : next.tier,
      date: next.date === DEFAULT_DATE_RANGE ? null : next.date,
    });
  }

  function handleToggleBrawler(id: number) {
    const next = selectedBrawlerId === id ? null : id;
    setSelectedBrawlerId(next);
    applySearchShallow({ brawlerId: next === null ? null : String(next) });
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-bold">
            {isRank ? "이벤트 통계 (순위형)" : "이벤트 통계"}
          </h2>
          <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[filter.tier]} 기준</p>
        </div>
        <StatRangeSelectorView
          tier={filter.tier}
          date={filter.date}
          onSelect={handleFilterChange}
        />
      </div>
      {isRank ? (
        <EventRankStatsTable
          items={rankStats.data ?? []}
          selectedBrawlerId={selectedBrawlerId}
          onToggleBrawler={handleToggleBrawler}
        />
      ) : (
        <EventResultStatsTable
          items={resultStats.data?.items ?? []}
          selectedBrawlerId={selectedBrawlerId}
          onToggleBrawler={handleToggleBrawler}
        />
      )}
      {selectedBrawlerId !== null && (
        <EventSelectedBrawlerPanel
          eventId={eventId}
          brawlerId={selectedBrawlerId}
          resultType={resultType}
          filter={filter}
          onClose={() => handleToggleBrawler(selectedBrawlerId)}
        />
      )}
    </section>
  );
}
