import type {
  BrawlerEnemyResultStatisticsItemResponse,
  BrawlerItemOwnershipStatisticsResponse,
  BrawlerPairResultStatisticsItemResponse,
  BrawlerRankStatisticsResponse,
  BrawlerResultDailyStatisticsResponse,
  BrawlerResultStatisticsItemResponse,
  BrawlerResultStatisticsResponse,
  BrawlerResponse,
  BrawlerTrioResultStatisticsItemResponse,
  ComboResultRankingItemResponse,
} from "@/api/types";
import { toBrawler } from "@/domain/brawler/mappers";
import type {
  BrawlerRankStat,
  BrawlerResultStat,
  BrawlerResultStatistics,
  ComboRankingStat,
  DailyResultStat,
  EnemyResultStat,
  ItemOwnership,
  PairResultStat,
  TrioResultStat,
} from "@/domain/statistics/types";
import { parseBrawlerTier, parseTier } from "@/domain/statistics/tier";

function toBrawlerResultStat(dto: BrawlerResultStatisticsItemResponse): BrawlerResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    battleCount: dto.battleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    starPlayerRate: dto.starPlayerRate,
    tierScore: dto.tierScore,
    tier: parseBrawlerTier(dto.tier),
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
    battleCount: dto.battleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    tierScore: dto.tierScore,
    tier: parseTier(dto.tier),
  };
}

function toBrawlers(dtos: (BrawlerResponse | null)[]) {
  return dtos.filter((b): b is BrawlerResponse => b !== null).map(toBrawler);
}

export function toTrioResultStat(
  dto: BrawlerTrioResultStatisticsItemResponse,
): TrioResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    pairBrawlers: toBrawlers(dto.pairBrawlers),
    battleCount: dto.battleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    tierScore: dto.tierScore,
    tier: parseTier(dto.tier),
  };
}

export function toComboRankingStat(dto: ComboResultRankingItemResponse): ComboRankingStat {
  return {
    brawlers: toBrawlers(dto.brawlers),
    battleCount: dto.battleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    tierScore: dto.tierScore,
    tier: parseTier(dto.tier),
  };
}

export function toEnemyResultStat(
  dto: BrawlerEnemyResultStatisticsItemResponse,
): EnemyResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    enemyBrawler: dto.enemyBrawler ? toBrawler(dto.enemyBrawler) : null,
    battleCount: dto.battleCount,
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
