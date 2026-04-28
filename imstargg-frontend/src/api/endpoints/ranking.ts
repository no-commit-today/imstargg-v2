import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlerRankingResponse,
  ClubRankingResponse,
  ListResponse,
  PlayerRankingResponse,
} from "@/api/types";

export const rankingApi = {
  players: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerRankingResponse>>(
      `/v1/ranking/players?country=${encodeURIComponent(country)}`,
      opts,
    ),

  brawlers: (brawlerId: number, country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerRankingResponse>>(
      `/v1/ranking/brawlers/${brawlerId}?country=${encodeURIComponent(country)}`,
      opts,
    ),

  clubs: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<ClubRankingResponse>>(
      `/v1/ranking/clubs?country=${encodeURIComponent(country)}`,
      opts,
    ),
};
