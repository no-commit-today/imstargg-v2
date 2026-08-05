"use client";

import { useEffect, useRef } from "react";
import {
  useMutation,
  useMutationState,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
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
    refetchInterval: (q) => (q.state.data?.progressing ? 500 : false),
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

  // 한 화면에서 이 훅을 여러 컴포넌트가 호출한다(버튼과 상태 캡션 등). mutationKey 로 상태를 공유해야
  // 버튼에서 낸 요청의 실패를 캡션 쪽 인스턴스도 볼 수 있다.
  const mutationKey = [...c.renewalKey(tag), "trigger"];
  const trigger = useMutation({
    mutationKey,
    mutationFn: () => c.renew(tag),
    onSuccess: (data) => {
      qc.setQueryData(c.renewalKey(tag), data);
    },
  });
  const triggerStatuses = useMutationState({
    filters: { mutationKey },
    select: (m) => m.state.status,
  });
  const lastTriggerStatus = triggerStatuses[triggerStatuses.length - 1];

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: lastTriggerStatus === "pending",
    triggerFailed: lastTriggerStatus === "error",
  };
}
