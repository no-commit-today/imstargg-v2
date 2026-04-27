"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function usePlayerRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.player.renewal(tag),
    queryFn: async ({ signal }) => {
      try {
        return await renewalApi.getPlayer(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) =>
      q.state.data?.progressing ? 1000 : false,
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.player.all });
      toast.success("갱신 완료");
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewPlayer(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.player.renewal(tag), data);
      toast.info("갱신 요청됨");
    },
    onError: () => toast.error("갱신 요청 실패"),
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
