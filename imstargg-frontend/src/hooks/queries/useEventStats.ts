"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
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
