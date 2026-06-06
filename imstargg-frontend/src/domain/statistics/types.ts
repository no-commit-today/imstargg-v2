import type { Brawler } from "@/domain/brawler/types";
import type { StatTier } from "@/domain/statistics/tier";

export interface BrawlerResultStat {
  brawler: Brawler | null;
  battleCount: number;
  pickRate: number;
  winRate: number;
  starPlayerRate: number;
  tierScore: number;
  tier: StatTier;
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
}

export interface PairRankStat {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;
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
