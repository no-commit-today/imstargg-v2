import type {
  BrawlerRarity, BrawlerRole, BuffyResponse, BrawlerResponse,
} from "@/api/types";

export interface PlayerClub {
  tag: string;
  name: string;
}

export interface Player {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  totalPrestigeLevel: number;
  club: PlayerClub | null;
}

export interface PlayerBrawler {
  id: number | null;
  name: string;
  rarity: BrawlerRarity | null;
  role: BrawlerRole | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  portraitImageUrl: string | null;
  buffies: BuffyResponse[];
  raw: BrawlerResponse | null;
}
