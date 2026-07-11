"use client";

import { useQuery } from "@tanstack/react-query";
import { playerStatisticsApi } from "@/api/endpoints/playerStatistics";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { toPlayerBrawlerStat, toPlayerBrawlerRoleStat } from "@/domain/statistics/player";

export type PlayerStatBattleType = "RANKED" | "SOLO_RANKED";

interface BrawlerStatsParams {
  tag: string;
  battleType: PlayerStatBattleType;
  brawlPassSeasonNumber: number | null;
  soloRankSeasonKey: string | null;
}

function resolveSeasonId(
  battleType: PlayerStatBattleType,
  brawlPassSeasonNumber: number | null,
  soloRankSeasonKey: string | null,
): string {
  return battleType === "RANKED"
    ? brawlPassSeasonNumber?.toString() ?? ""
    : soloRankSeasonKey ?? "";
}

export function usePlayerBrawlerStatistics({
  tag,
  battleType,
  brawlPassSeasonNumber,
  soloRankSeasonKey,
}: BrawlerStatsParams) {
  const seasonId = resolveSeasonId(battleType, brawlPassSeasonNumber, soloRankSeasonKey);
  const enabled = seasonId !== "";
  return useQuery({
    queryKey: queryKeys.player.statistics.brawler(tag, battleType, seasonId),
    queryFn: ({ signal }) =>
      battleType === "RANKED"
        ? playerStatisticsApi.rankedBrawler(tag, brawlPassSeasonNumber!, { signal })
        : playerStatisticsApi.soloRankedBrawler(tag, soloRankSeasonKey!, { signal }),
    select: (dto) => dto.items.map(toPlayerBrawlerStat).sort((a, b) => b.winRate - a.winRate),
    enabled,
  });
}

export function usePlayerBrawlerRoleStatistics({
  tag,
  battleType,
  brawlPassSeasonNumber,
  soloRankSeasonKey,
}: BrawlerStatsParams) {
  const seasonId = resolveSeasonId(battleType, brawlPassSeasonNumber, soloRankSeasonKey);
  const enabled = seasonId !== "";
  return useQuery({
    queryKey: queryKeys.player.statistics.role(tag, battleType, seasonId),
    queryFn: ({ signal }) =>
      battleType === "RANKED"
        ? playerStatisticsApi.rankedRole(tag, brawlPassSeasonNumber!, { signal })
        : playerStatisticsApi.soloRankedRole(tag, soloRankSeasonKey!, { signal }),
    select: (dto) => dto.items.map(toPlayerBrawlerRoleStat),
    enabled,
  });
}
