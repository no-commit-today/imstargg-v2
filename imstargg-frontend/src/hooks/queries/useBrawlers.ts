"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBrawler } from "@/domain/brawler/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawlers(options?: { initialData?: Brawler[] }) {
  return useQuery({
    queryKey: queryKeys.brawler.list(),
    queryFn: ({ signal }) =>
      brawlerApi.list({ signal }).then((r) => r.content.map(toBrawler)),
    initialData: options?.initialData,
    staleTime: QUERY_STALE_TIME.BRAWLER_REFERENCE,
  });
}
