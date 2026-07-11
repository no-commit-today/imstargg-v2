"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import type { BattleEventModeResultType } from "@/api/types";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
  toEnemyResultStat,
  toPairRankStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type {
  BrawlerRankStat,
  BrawlerResultStatistics,
} from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { isDefaultStatsFilter, type StatsFilter } from "./useBrawlerStats";

export function useEventResultStats(
  eventId: number,
  filter: StatsFilter,
  initial?: BrawlerResultStatistics,
  enabled = true,
) {
  const params = { eventId, tierRange: filter.tier, dateRange: filter.date };
  return useQuery({
    queryKey: queryKeys.statistics.brawlerResult(params),
    queryFn: ({ signal }) =>
      statisticsApi.brawlerResult(params, { signal }).then(toBrawlerResultStatistics),
    initialData: isDefaultStatsFilter(filter) ? initial : undefined,
    enabled,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}

export function useEventRankStats(
  eventId: number,
  filter: StatsFilter,
  initial?: BrawlerRankStat[],
  enabled = true,
) {
  const params = { eventId, tierRange: filter.tier, dateRange: filter.date };
  return useQuery({
    queryKey: queryKeys.statistics.brawlerRank(params),
    queryFn: ({ signal }) =>
      statisticsApi
        .brawlerRank(params, { signal })
        .then((r) => r.content.map(toBrawlerRankStat)),
    initialData: isDefaultStatsFilter(filter) ? initial : undefined,
    enabled,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}

export function useEventSelectedStats(
  eventId: number,
  brawlerId: number | null,
  resultType: BattleEventModeResultType,
  filter: StatsFilter,
) {
  const selected = brawlerId !== null;
  const base = { eventId, tierRange: filter.tier, dateRange: filter.date };
  const common = { staleTime: QUERY_STALE_TIME.SHORT } as const;

  // enabled 가드로 brawlerId=null 인 동안 queryFn 이 실행되지 않는다.
  const pairResult = useQuery({
    queryKey: queryKeys.statistics.pairResult({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .pairResult({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toPairResultStat)),
    enabled: selected && resultType === "RESULT",
    ...common,
  });
  const enemyResult = useQuery({
    queryKey: queryKeys.statistics.enemyResult({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .enemyResult({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toEnemyResultStat)),
    enabled: selected && resultType === "RESULT",
    ...common,
  });
  const pairRank = useQuery({
    queryKey: queryKeys.statistics.pairRank({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .pairRank({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toPairRankStat)),
    enabled: selected && resultType === "RANK",
    ...common,
  });

  return { pairResult, enemyResult, pairRank };
}
