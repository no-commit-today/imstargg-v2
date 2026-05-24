import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  ListResponse,
  PlayerBrawlerResponse,
  PlayerResponse,
  SliceResponse,
} from "@/api/types";

export const playerApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<PlayerResponse>(`/v1/players/${toUrlPath(tag)}`, opts),

  getBrawlers: (tag: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerBrawlerResponse>>(
      `/v1/players/${toUrlPath(tag)}/brawlers`,
      opts,
    ),

  search: (
    q: string,
    offset: number,
    limit: number,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<PlayerResponse>>(
      `/v1/player/search?q=${encodeURIComponent(q)}&offset=${offset}&limit=${limit}`,
      opts,
    ),
};
