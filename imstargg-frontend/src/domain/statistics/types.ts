import type { Brawler } from "@/domain/brawler/types";
import type { StatTier } from "@/domain/statistics/tier";

export interface BrawlerResultStat {
  brawler: Brawler | null;
  battleCount: number;
  pickRate: number;
  winRate: number;
  starPlayerRate: number;
  tierScore: number;
  tier: StatTier | null;
}

export interface BrawlerResultStatistics {
  totalBattleCount: number;
  items: BrawlerResultStat[];
}

export interface BrawlerRankStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface PairResultStat {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  battleCount: number;
  pickRate: number;
  winRate: number;
  tierScore: number;
  tier: StatTier;
}

export interface TrioResultStat {
  brawler: Brawler | null;
  pairBrawlers: Brawler[];
  battleCount: number;
  pickRate: number;
  winRate: number;
  tierScore: number;
  tier: StatTier;
}

/** 전체 2인·3인 조합 정렬의 한 행. `brawlers` 길이는 2 또는 3. */
export interface ComboRankingStat {
  brawlers: Brawler[];
  battleCount: number;
  pickRate: number;
  winRate: number;
  tierScore: number;
  tier: StatTier;
}

export interface EnemyResultStat {
  brawler: Brawler | null;
  enemyBrawler: Brawler | null;
  battleCount: number;
  pickRate: number;
  winRate: number;
}

export interface DailyResultStat {
  date: string;
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
}

export interface ItemOwnershipItem {
  itemId: number;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BuffyOwnershipItem {
  buffyType: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface ItemOwnership {
  gears: ItemOwnershipItem[];
  gadgets: ItemOwnershipItem[];
  starPowers: ItemOwnershipItem[];
  hyperCharges: ItemOwnershipItem[];
  buffies: BuffyOwnershipItem[];
}
