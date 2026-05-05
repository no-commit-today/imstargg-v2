import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlPassSeasonResponse,
  ListResponse,
  SoloRankSeasonResponse,
} from "@/api/types";

export const seasonApi = {
  brawlPass: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlPassSeasonResponse>>("/v1/seasons/brawl-pass", opts),
  soloRank: (opts?: FetchOpts) =>
    apiFetch<ListResponse<SoloRankSeasonResponse>>("/v1/seasons/solo-rank", opts),
};
