import type {
  BrawlerEnemyResultStatisticsItemResponse,
  BrawlerItemOwnershipStatisticsResponse,
  BrawlerPairRankStatisticsResponse,
  BrawlerPairResultStatisticsItemResponse,
  BrawlerRankStatisticsResponse,
  BrawlerResultDailyStatisticsResponse,
  BrawlerResultStatisticsItemResponse,
  BrawlerResultStatisticsResponse,
} from "@/api/types";
import { toBrawler } from "@/domain/brawler/mappers";
import type {
  BrawlerRankStat,
  BrawlerResultStat,
  BrawlerResultStatistics,
  DailyResultStat,
  EnemyResultStat,
  ItemOwnership,
  PairRankStat,
  PairResultStat,
} from "@/domain/statistics/types";
import { parseTier } from "@/domain/statistics/tier";

function toBrawlerResultStat(dto: BrawlerResultStatisticsItemResponse): BrawlerResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    starPlayerRate: dto.starPlayerRate,
    tierScore: dto.tierScore,
    tier: parseTier(dto.tier),
  };
}

export function toBrawlerResultStatistics(
  dto: BrawlerResultStatisticsResponse,
): BrawlerResultStatistics {
  return {
    totalBattleCount: dto.totalBattleCount,
    items: dto.items.map(toBrawlerResultStat),
  };
}

export function toBrawlerRankStat(dto: BrawlerRankStatisticsResponse): BrawlerRankStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    averageRank: dto.averageRank,
  };
}

export function toPairResultStat(
  dto: BrawlerPairResultStatisticsItemResponse,
): PairResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    pairBrawler: dto.pairBrawler ? toBrawler(dto.pairBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
  };
}

export function toPairRankStat(
  dto: BrawlerPairRankStatisticsResponse,
): PairRankStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    pairBrawler: dto.pairBrawler ? toBrawler(dto.pairBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    averageRank: dto.averageRank,
  };
}

export function toEnemyResultStat(
  dto: BrawlerEnemyResultStatisticsItemResponse,
): EnemyResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    enemyBrawler: dto.enemyBrawler ? toBrawler(dto.enemyBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
  };
}

export function toDailyResultStat(
  dto: BrawlerResultDailyStatisticsResponse,
): DailyResultStat {
  return {
    date: dto.date,
    totalBattleCount: dto.totalBattleCount,
    winRate: dto.winRate,
    starPlayerRate: dto.starPlayerRate,
  };
}

export function toItemOwnership(
  dto: BrawlerItemOwnershipStatisticsResponse,
): ItemOwnership {
  return {
    gears: dto.gears,
    gadgets: dto.gadgets,
    starPowers: dto.starPowers,
    hyperCharges: dto.hyperCharges,
    buffies: dto.buffies,
  };
}
