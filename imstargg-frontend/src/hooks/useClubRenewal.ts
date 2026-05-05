"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useClubRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.club.renewal(tag),
    queryFn: async ({ signal }) => {
      try {
        return await renewalApi.getClub(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.club.all });
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewClub(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.club.renewal(tag), data);
    },
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
