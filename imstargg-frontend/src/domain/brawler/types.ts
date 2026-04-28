import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
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
  hyperCharges: BrawlerItem[];
  buffies: BuffyResponse[];
}
