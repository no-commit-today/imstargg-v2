"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  type StatsDateRange,
  type StatsTierRange,
} from "@/domain/statistics/enums";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type {
  DailyResultStat,
  EnemyResultStat,
  PairResultStat,
} from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export interface StatsFilter {
  tier: StatsTierRange;
  date: StatsDateRange;
}

export const DEFAULT_STATS_FILTER: StatsFilter = {
  tier: DEFAULT_TIER_RANGE,
  date: DEFAULT_DATE_RANGE,
};

export function isDefaultStatsFilter(filter: StatsFilter): boolean {
  return filter.tier === DEFAULT_TIER_RANGE && filter.date === DEFAULT_DATE_RANGE;
}

export interface BrawlerStatsInitial {
  daily: DailyResultStat[];
  pair: PairResultStat[];
  enemy: EnemyResultStat[];
}

export function useBrawlerStats(
  brawlerId: number,
  filter: StatsFilter,
  initial?: BrawlerStatsInitial,
) {
  const params = { brawlerId, tierRange: filter.tier, dateRange: filter.date };
  const useInitial = isDefaultStatsFilter(filter) ? initial : undefined;
  const common = {
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  } as const;

  const daily = useQuery({
    queryKey: queryKeys.statistics.daily(params),
    queryFn: ({ signal }) =>
      statisticsApi.daily(params, { signal }).then((r) => r.content.map(toDailyResultStat)),
    initialData: useInitial?.daily,
    ...common,
  });
  const pair = useQuery({
    queryKey: queryKeys.statistics.pairResult(params),
    queryFn: ({ signal }) =>
      statisticsApi.pairResult(params, { signal }).then((r) => r.content.map(toPairResultStat)),
    initialData: useInitial?.pair,
    ...common,
  });
  const enemy = useQuery({
    queryKey: queryKeys.statistics.enemyResult(params),
    queryFn: ({ signal }) =>
      statisticsApi
        .enemyResult(params, { signal })
        .then((r) => r.content.map(toEnemyResultStat)),
    initialData: useInitial?.enemy,
    ...common,
  });

  return { daily, pair, enemy };
}
