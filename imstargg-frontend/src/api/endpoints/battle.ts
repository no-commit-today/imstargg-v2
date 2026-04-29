import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { BattleResponse, SliceResponse } from "@/api/types";

export interface PlayerBattlesParams {
  offset: number;
  limit: number;
}

function buildQuery(params: object): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null) continue;
    sp.set(k, String(v));
  }
  const qs = sp.toString();
  return qs ? `?${qs}` : "";
}

export const battleApi = {
  getPlayerBattles: (
    tag: string,
    params: PlayerBattlesParams,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<BattleResponse>>(
      `/v1/players/${toUrlPath(tag)}/battles${buildQuery(params)}`,
      opts,
    ),
};
