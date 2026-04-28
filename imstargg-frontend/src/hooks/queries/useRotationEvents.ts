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
    staleTime: 5 * 60_000,
  });
}
