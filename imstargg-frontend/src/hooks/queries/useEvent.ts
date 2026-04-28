"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useEvent(id: number | string, options?: { initialData?: BattleEvent }) {
  return useQuery({
    queryKey: queryKeys.event.detail(id),
    queryFn: ({ signal }) => eventApi.get(id, { signal }).then(toBattleEvent),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
