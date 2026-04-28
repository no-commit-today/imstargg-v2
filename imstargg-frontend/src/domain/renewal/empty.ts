import type { RenewalResponse } from "@/api/types";
import { normalizeTag } from "@/lib/tag";

/**
 * 갱신 기록이 없는 (첫 갱신 전) 상태를 표현하는 합성 응답.
 * 백엔드는 GET /v1/{players|clubs}/{tag}/renewal 에 대해 NOT_FOUND(404)를 반환하지만
 * POST 는 첫 갱신 생성을 허용하므로, 클라이언트에서 canRenew=true 로 다룬다.
 */
export function emptyRenewal(tag: string): RenewalResponse {
  return {
    tag: normalizeTag(tag),
    status: "NOT_FOUND",
    progressing: false,
    canRenew: true,
    requestedAt: new Date(0).toISOString(),
    executedAt: null,
    completedAt: null,
    renewedAt: null,
  };
}
