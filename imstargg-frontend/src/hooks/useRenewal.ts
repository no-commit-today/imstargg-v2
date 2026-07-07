"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export type RenewalKind = "player" | "club";

const config = {
  player: {
    renewalKey: queryKeys.player.renewal,
    allKey: queryKeys.player.all,
    get: renewalApi.getPlayer,
    renew: renewalApi.renewPlayer,
  },
  club: {
    renewalKey: queryKeys.club.renewal,
    allKey: queryKeys.club.all,
    get: renewalApi.getClub,
    renew: renewalApi.renewClub,
  },
} as const;

export function useRenewal(kind: RenewalKind, tag: string) {
  const qc = useQueryClient();
  const c = config[kind];

  const renewalQuery = useQuery({
    queryKey: c.renewalKey(tag),
    queryFn: async ({ signal }) => {
      try {
        return await c.get(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(renewalQuery.data?.progressing ?? false);
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: c.allKey });
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc, c.allKey]);

  const trigger = useMutation({
    mutationFn: () => c.renew(tag),
    onSuccess: (data) => {
      qc.setQueryData(c.renewalKey(tag), data);
    },
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
    triggerFailed: trigger.isError,
  };
}
