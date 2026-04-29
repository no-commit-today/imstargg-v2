import type { BattleMode, BattleType, BattleResult, SoloRankTier } from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";

export type { BattleMode, BattleType, BattleResult, SoloRankTier };

export interface BattlePlayerBrawler {
  brawler: Brawler | null;
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

export interface BattlePlayer {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawler;
}

export interface BattleEvent {
  id: number;
  modeName: string | null;
  modeIconUrl: string | null;
  modeResultType: "RESULT" | "RANK" | null;
  mapName: string;
}

export type BattleResultType = "RESULT" | "RANK";

export interface Battle {
  battleTime: string;
  event: BattleEvent | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;
  starPlayerTag: string | null;
  rank: number | null;
  trophyChange: number | null;
  teams: BattlePlayer[][];
  /** 화면 분기용 파생값. rank != null 이면 RANK, 아니면 RESULT */
  resultType: BattleResultType;
}
