"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { QUERY_STALE_TIME } from "@/config/cache";

export function useEventSearch(name: string) {
  const trimmed = name.trim();
  return useQuery({
    queryKey: queryKeys.event.search(trimmed),
    queryFn: ({ signal }) =>
      eventApi.search(trimmed, { signal }).then((r) => r.content.map(toBattleEvent)),
    enabled: trimmed.length > 0,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
