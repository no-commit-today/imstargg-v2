import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BattleEventResponse,
  ListResponse,
  RotationBattleEventResponse,
} from "@/api/types";

export const eventApi = {
  rotation: (opts?: FetchOpts) =>
    apiFetch<ListResponse<RotationBattleEventResponse>>(`/v1/events/rotation`, opts),

  soloRank: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(`/v1/events/solo-rank`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BattleEventResponse>(`/v1/events/${id}`, opts),

  search: (name: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(
      `/v1/event/search?name=${encodeURIComponent(name)}`,
      opts,
    ),
};
