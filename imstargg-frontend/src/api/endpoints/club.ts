import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { ClubResponse } from "@/api/types";

export const clubApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<ClubResponse>(`/v1/clubs/${toUrlPath(tag)}`, opts),
};
