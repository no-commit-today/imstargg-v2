import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerRole, BrawlerRarity } from "@/api/types";

export interface BrawlerFilterOptions {
  role?: BrawlerRole;
  rarity?: BrawlerRarity;
  search?: string;
}

export function filterBrawlers(
  list: Brawler[],
  opts: BrawlerFilterOptions,
): Brawler[] {
  const search = opts.search?.trim().toLocaleLowerCase("ko") ?? "";
  return list.filter((b) => {
    if (opts.role && b.role !== opts.role) return false;
    if (opts.rarity && b.rarity !== opts.rarity) return false;
    if (search && !b.name.toLocaleLowerCase("ko").includes(search)) return false;
    return true;
  });
}

export function sortByName(list: Brawler[]): Brawler[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name, "ko"));
}
