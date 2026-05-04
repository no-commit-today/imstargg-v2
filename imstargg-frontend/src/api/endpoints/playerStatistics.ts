import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  PlayerBrawlerRoleStatisticsResponse,
  PlayerBrawlerStatisticsResponse,
} from "@/api/types";

export const playerStatisticsApi = {
  rankedBrawler: (tag: string, seasonNumber: number, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler/ranked?seasonNumber=${seasonNumber}`,
      opts,
    ),
  soloRankedBrawler: (tag: string, seasonKey: string, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler/solo-ranked?seasonKey=${encodeURIComponent(seasonKey)}`,
      opts,
    ),
  rankedRole: (tag: string, seasonNumber: number, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerRoleStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler-role/ranked?seasonNumber=${seasonNumber}`,
      opts,
    ),
  soloRankedRole: (tag: string, seasonKey: string, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerRoleStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler-role/solo-ranked?seasonKey=${encodeURIComponent(seasonKey)}`,
      opts,
    ),
};
