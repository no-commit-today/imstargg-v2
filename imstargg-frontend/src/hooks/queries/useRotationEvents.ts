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
    // initialData를 즉시 stale로 간주하여 mount/focus 시 항상 최신 fetch 시도.
    initialDataUpdatedAt: 0,
    staleTime: 30_000,
    refetchOnWindowFocus: "always",
    refetchOnReconnect: "always",
    refetchInterval: (query) => {
      const data = query.state.data as RotationEvent[] | undefined;
      if (!data || data.length === 0) return false;
      const minEndMs = Math.min(...data.map((r) => new Date(r.endTime).getTime()));
      const remaining = minEndMs - Date.now();
      // 가장 빨리 만료되는 이벤트 종료 2초 후 refetch — 만료된 카드가 즉시 사라지도록.
      // 이미 만료된 상태라면 5초 후 재시도.
      return remaining > 0 ? remaining + 2_000 : 5_000;
    },
    // tab이 hidden 상태여도 interval 유지 — 복귀 시 만료된 카드가 그대로 보이지 않도록.
    refetchIntervalInBackground: true,
  });
}
