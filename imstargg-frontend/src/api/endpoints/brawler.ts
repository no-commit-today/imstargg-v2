import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerResponse>(`/v1/brawlers/${id}`, opts),
};
