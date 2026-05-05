import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerResponse, BrawlerWithItemsResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerWithItemsResponse>(`/v1/brawlers/${id}`, opts),
};
