"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBrawlerWithItems } from "@/domain/brawler/mappers";
import type { BrawlerWithItems } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawler(
  id: number | string,
  options?: { initialData?: BrawlerWithItems },
) {
  return useQuery({
    queryKey: queryKeys.brawler.detail(id),
    queryFn: ({ signal }) => brawlerApi.get(id, { signal }).then(toBrawlerWithItems),
    initialData: options?.initialData,
    staleTime: QUERY_STALE_TIME.BRAWLER_REFERENCE,
  });
}
