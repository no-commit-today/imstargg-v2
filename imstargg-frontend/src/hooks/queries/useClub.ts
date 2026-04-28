"use client";

import { useQuery } from "@tanstack/react-query";
import { clubApi } from "@/api/endpoints/club";
import { toClub } from "@/domain/club/mappers";
import type { Club } from "@/domain/club/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useClub(tag: string, options?: { initialData?: Club }) {
  return useQuery({
    queryKey: queryKeys.club.detail(tag),
    queryFn: ({ signal }) => clubApi.get(tag, { signal }).then(toClub),
    initialData: options?.initialData,
    staleTime: 60_000,
  });
}
