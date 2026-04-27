import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  ListResponse,
} from "@/api/types";

export const playerApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<PlayerResponse>(`/v1/players/${toUrlPath(tag)}`, opts),

  getBrawlers: (tag: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerBrawlerResponse>>(
      `/v1/players/${toUrlPath(tag)}/brawlers`,
      opts,
    ),
};
