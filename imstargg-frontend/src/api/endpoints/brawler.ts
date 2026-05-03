import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerDetailResponse, BrawlerResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerDetailResponse>(`/v1/brawlers/${id}`, opts),
};
