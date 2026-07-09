"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  DEFAULT_TROPHY_RANGE,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import { toItemOwnership } from "@/domain/statistics/mappers";
import type { ItemOwnership } from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useItemOwnership(
  brawlerId: number,
  trophy: BrawlerTrophyRange,
  initial?: ItemOwnership,
) {
  const params = { brawlerId, trophyRange: trophy };
  return useQuery({
    queryKey: queryKeys.statistics.itemOwnership(params),
    queryFn: ({ signal }) =>
      statisticsApi.itemOwnership(params, { signal }).then(toItemOwnership),
    initialData: trophy === DEFAULT_TROPHY_RANGE ? initial : undefined,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}
