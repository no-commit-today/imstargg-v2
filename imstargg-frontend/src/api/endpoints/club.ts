import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { ClubResponse, SliceResponse } from "@/api/types";

export const clubApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<ClubResponse>(`/v1/clubs/${toUrlPath(tag)}`, opts),

  search: (
    q: string,
    offset: number,
    limit: number,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<ClubResponse>>(
      `/v1/club/search?q=${encodeURIComponent(q)}&offset=${offset}&limit=${limit}`,
      opts,
    ),
};
