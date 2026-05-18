import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlerEnemyResultStatisticsItemResponse,
  BrawlerItemOwnershipStatisticsResponse,
  BrawlerPairRankStatisticsResponse,
  BrawlerPairResultStatisticsItemResponse,
  BrawlerRankStatisticsResponse,
  BrawlerResultDailyStatisticsResponse,
  BrawlerResultStatisticsResponse,
  BrawlerTrophyRangeDto,
  ListResponse,
  StatsDateRangeDto,
  StatsTierRangeDto,
} from "@/api/types";

function buildQuery(params: object): string {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null,
  );
  if (entries.length === 0) return "";
  const sp = new URLSearchParams();
  for (const [k, v] of entries) sp.set(k, String(v));
  return `?${sp.toString()}`;
}

export interface BrawlerResultParams {
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
}

export interface BrawlerRankParams {
  eventId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
}

export interface PairResultParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
}

export interface PairRankParams {
  eventId: number;
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
}

export interface EnemyResultParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
}

export interface DailyParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
}

export interface ItemOwnershipParams {
  brawlerId: number;
  trophyRange: BrawlerTrophyRangeDto;
}

export const statisticsApi = {
  brawlerResult: (p: BrawlerResultParams, opts?: FetchOpts) =>
    apiFetch<BrawlerResultStatisticsResponse>(
      `/v1/statistics/brawler/result${buildQuery(p)}`,
      opts,
    ),

  brawlerRank: (p: BrawlerRankParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerRankStatisticsResponse>>(
      `/v1/statistics/brawler/rank${buildQuery(p)}`,
      opts,
    ),

  pairResult: (p: PairResultParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerPairResultStatisticsItemResponse>>(
      `/v1/statistics/brawler/pair/result${buildQuery(p)}`,
      opts,
    ),

  pairRank: (p: PairRankParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerPairRankStatisticsResponse>>(
      `/v1/statistics/brawler/pair/rank${buildQuery(p)}`,
      opts,
    ),

  enemyResult: (p: EnemyResultParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerEnemyResultStatisticsItemResponse>>(
      `/v1/statistics/brawler/enemy/result${buildQuery(p)}`,
      opts,
    ),

  daily: (p: DailyParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResultDailyStatisticsResponse>>(
      `/v1/statistics/brawler/result/daily${buildQuery(p)}`,
      opts,
    ),

  itemOwnership: (p: ItemOwnershipParams, opts?: FetchOpts) =>
    apiFetch<BrawlerItemOwnershipStatisticsResponse>(
      `/v1/statistics/brawler/item/ownership${buildQuery(p)}`,
      opts,
    ),
};
