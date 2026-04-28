"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawler(
  id: number | string,
  options?: { initialData?: Brawler },
) {
  return useQuery({
    queryKey: queryKeys.brawler.detail(id),
    queryFn: ({ signal }) => brawlerApi.get(id, { signal }).then(toBrawler),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
