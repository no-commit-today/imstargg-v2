"use client";

import { useMutation } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import type { RenewalKind } from "@/hooks/useRenewal";

/**
 * 태그를 직접 입력해 갱신을 요청하는 폼(RenewalCard)용 mutation 훅.
 * 폴링 훅 useRenewal 과 달리 "요청 후 결과 페이지로 이동" 플로우 전용.
 */
export function useRenewalSubmit(kind: RenewalKind) {
  return useMutation({
    mutationFn: (tag: string) =>
      kind === "player" ? renewalApi.renewPlayer(tag) : renewalApi.renewClub(tag),
  });
}
