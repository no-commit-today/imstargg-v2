"use client";

import { useQuery } from "@tanstack/react-query";
import { seasonApi } from "@/api/endpoints/season";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawlPassSeasons() {
  return useQuery({
    queryKey: queryKeys.season.brawlPass(),
    queryFn: ({ signal }) => seasonApi.brawlPass({ signal }),
    staleTime: Infinity,
  });
}

export function useSoloRankSeasons() {
  return useQuery({
    queryKey: queryKeys.season.soloRank(),
    queryFn: ({ signal }) => seasonApi.soloRank({ signal }),
    staleTime: Infinity,
  });
}
