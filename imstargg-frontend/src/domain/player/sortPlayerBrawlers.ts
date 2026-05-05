import type { BrawlerRarity } from "@/api/types";
import type { PlayerBrawler } from "./types";

export type PlayerBrawlerSortKey = "trophies" | "power" | "rarity" | "name" | "id";
export type SortDirection = "asc" | "desc";

const RARITY_ORDER: Record<BrawlerRarity, number> = {
  STARTING_BRAWLER: 0,
  RARE: 1,
  SUPER_RARE: 2,
  EPIC: 3,
  MYTHIC: 4,
  LEGENDARY: 5,
  ULTRA_LEGENDARY: 6,
};

function pickValue(pb: PlayerBrawler, key: PlayerBrawlerSortKey): number | string {
  switch (key) {
    case "trophies":
      return pb.trophies;
    case "power":
      return pb.power;
    case "rarity":
      return pb.brawler ? RARITY_ORDER[pb.brawler.rarity] : -1;
    case "name":
      return pb.brawler?.name ?? "";
    case "id":
      return pb.brawler?.id ?? 0;
  }
}

export function sortPlayerBrawlers(
  brawlers: PlayerBrawler[],
  key: PlayerBrawlerSortKey,
  direction: SortDirection,
): PlayerBrawler[] {
  const sign = direction === "asc" ? 1 : -1;
  return [...brawlers].sort((a, b) => {
    const av = pickValue(a, key);
    const bv = pickValue(b, key);
    if (typeof av === "string" && typeof bv === "string") {
      return sign * av.localeCompare(bv);
    }
    return sign * ((av as number) - (bv as number));
  });
}
