import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { RenewalResponse } from "@/api/types";

export const renewalApi = {
  getPlayer: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/players/${toUrlPath(tag)}/renewal`, opts),

  renewPlayer: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/players/${toUrlPath(tag)}/renewal`, {
      method: "POST",
    }),

  getClub: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${toUrlPath(tag)}/renewal`, opts),

  renewClub: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${toUrlPath(tag)}/renewal`, {
      method: "POST",
    }),
};
