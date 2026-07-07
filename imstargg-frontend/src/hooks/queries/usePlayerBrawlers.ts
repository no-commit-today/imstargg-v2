"use client";

import { useQuery } from "@tanstack/react-query";
import { playerApi } from "@/api/endpoints/player";
import { toPlayerBrawler } from "@/domain/player/mappers";
import type { PlayerBrawler } from "@/domain/player/types";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { QUERY_STALE_TIME } from "@/config/cache";

export function usePlayerBrawlers(
  tag: string,
  options?: { initialData?: PlayerBrawler[] },
) {
  return useQuery({
    queryKey: queryKeys.player.brawlers(tag),
    queryFn: ({ signal }) =>
      playerApi.getBrawlers(tag, { signal }).then((res) => res.content.map(toPlayerBrawler)),
    initialData: options?.initialData,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
