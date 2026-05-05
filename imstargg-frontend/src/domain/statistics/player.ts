import type {
  PlayerBrawlerRoleStatisticsItemResponse,
  PlayerBrawlerStatisticsItemResponse,
  BrawlerRole,
} from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";
import { toBrawler } from "@/domain/brawler/mappers";

export interface PlayerBrawlerStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export interface PlayerBrawlerRoleStat {
  role: BrawlerRole;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export function toPlayerBrawlerStat(
  dto: PlayerBrawlerStatisticsItemResponse,
): PlayerBrawlerStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    victoryCount: dto.victoryCount,
    defeatCount: dto.defeatCount,
    drawCount: dto.drawCount,
    winRate: dto.winRate,
  };
}

export function toPlayerBrawlerRoleStat(
  dto: PlayerBrawlerRoleStatisticsItemResponse,
): PlayerBrawlerRoleStat {
  return {
    role: dto.role,
    totalBattleCount: dto.totalBattleCount,
    victoryCount: dto.victoryCount,
    defeatCount: dto.defeatCount,
    drawCount: dto.drawCount,
    winRate: dto.winRate,
  };
}
