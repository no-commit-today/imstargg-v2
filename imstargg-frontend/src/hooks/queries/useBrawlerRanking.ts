"use client";

import { useQuery } from "@tanstack/react-query";
import { rankingApi } from "@/api/endpoints/ranking";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBrawlerRanking } from "@/domain/ranking/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawlerRanking(brawlerId: number, country: string) {
  return useQuery({
    queryKey: queryKeys.ranking.brawlers(brawlerId, country),
    queryFn: ({ signal }) =>
      rankingApi
        .brawlers(brawlerId, country, { signal })
        .then((res) => res.content.map(toBrawlerRanking)),
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
