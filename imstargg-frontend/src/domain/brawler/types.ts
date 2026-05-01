import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
}

export interface Buffies {
  gadget: BuffyResponse;
  starPower: BuffyResponse;
  hyperCharge: BuffyResponse;
}

export interface Brawler {
  id: number;
  name: string;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
  gadgets: BrawlerItem[];
  gears: BrawlerItem[];
  starPowers: BrawlerItem[];
  hyperCharge: BrawlerItem | null;
  buffies: Buffies | null;
}
