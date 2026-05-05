"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBattleEvent } from "@/domain/event/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useSoloRankEvents(options?: { initialData?: BattleEvent[] }) {
  return useQuery({
    queryKey: queryKeys.event.soloRank(),
    queryFn: ({ signal }) =>
      eventApi.soloRank({ signal }).then((r) => r.content.map(toBattleEvent)),
    initialData: options?.initialData,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
