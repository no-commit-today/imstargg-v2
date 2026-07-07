"use client";

import { useQuery } from "@tanstack/react-query";
import { playerApi } from "@/api/endpoints/player";
import { toPlayer } from "@/domain/player/mappers";
import type { Player } from "@/domain/player/types";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { QUERY_STALE_TIME } from "@/config/cache";

export function usePlayer(tag: string, options?: { initialData?: Player }) {
  return useQuery({
    queryKey: queryKeys.player.detail(tag),
    queryFn: ({ signal }) => playerApi.get(tag, { signal }).then(toPlayer),
    initialData: options?.initialData,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
