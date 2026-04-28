"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toRotationEvent } from "@/domain/event/mappers";
import type { RotationEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useRotationEvents(options?: { initialData?: RotationEvent[] }) {
  return useQuery({
    queryKey: queryKeys.event.rotation(),
    queryFn: ({ signal }) =>
      eventApi.rotation({ signal }).then((r) => r.content.map(toRotationEvent)),
    initialData: options?.initialData,
    staleTime: 30_000,
    refetchInterval: (query) => {
      const data = query.state.data as RotationEvent[] | undefined;
      if (!data || data.length === 0) return false;
      const minEndMs = Math.min(...data.map((r) => new Date(r.endTime).getTime()));
      const remaining = minEndMs - Date.now();
      // 가장 빨리 만료되는 이벤트 종료 2초 후 refetch — 만료된 카드가 즉시 사라지도록.
      // 이미 만료된 상태라면 5초 후 재시도.
      return remaining > 0 ? remaining + 2_000 : 5_000;
    },
  });
}
