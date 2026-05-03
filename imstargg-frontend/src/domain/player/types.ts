import type { BuffyResponse } from "@/api/types";
import type { Brawler, BrawlerItem } from "@/domain/brawler/types";

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

export interface PlayerBrawlerOwnedItem {
  item: BrawlerItem;
  owned: boolean;
}

export interface PlayerBrawlerOwnedBuffy {
  item: BuffyResponse;
  owned: boolean;
}

export interface PlayerBrawlerBuffies {
  gadget: PlayerBrawlerOwnedBuffy;
  starPower: PlayerBrawlerOwnedBuffy;
  hyperCharge: PlayerBrawlerOwnedBuffy;
}

export interface PlayerBrawler {
  brawler: Brawler | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  gadgets: PlayerBrawlerOwnedItem[];
  gears: PlayerBrawlerOwnedItem[];
  starPowers: PlayerBrawlerOwnedItem[];
  hyperCharge: PlayerBrawlerOwnedItem | null;
  buffies: PlayerBrawlerBuffies | null;
}

