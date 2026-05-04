import { normalizeTag } from "@/lib/tag";

export const queryKeys = {
  player: {
    all: ["player"] as const,
    detail: (tag: string) =>
      [...queryKeys.player.all, "detail", normalizeTag(tag)] as const,
    brawlers: (tag: string) =>
      [...queryKeys.player.all, "brawlers", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.player.all, "renewal", normalizeTag(tag)] as const,
    battles: (tag: string) =>
      [...queryKeys.player.all, "battles", normalizeTag(tag)] as const,
  },
  club: {
    all: ["club"] as const,
    detail: (tag: string) =>
      [...queryKeys.club.all, "detail", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.club.all, "renewal", normalizeTag(tag)] as const,
  },
  brawler: {
    all: ["brawler"] as const,
    list: () => [...queryKeys.brawler.all, "list"] as const,
    detail: (id: number | string) =>
      [...queryKeys.brawler.all, "detail", String(id)] as const,
  },
  event: {
    all: ["event"] as const,
    rotation: () => [...queryKeys.event.all, "rotation"] as const,
    soloRank: () => [...queryKeys.event.all, "solo-rank"] as const,
    detail: (id: number | string) =>
      [...queryKeys.event.all, "detail", String(id)] as const,
    search: (name: string) =>
      [...queryKeys.event.all, "search", name] as const,
  },
  ranking: {
    all: ["ranking"] as const,
    players: (country: string) =>
      [...queryKeys.ranking.all, "players", country] as const,
    brawlers: (brawlerId: number | string, country: string) =>
      [...queryKeys.ranking.all, "brawlers", String(brawlerId), country] as const,
    clubs: (country: string) =>
      [...queryKeys.ranking.all, "clubs", country] as const,
  },
  season: {
    all: ["season"] as const,
    brawlPass: () => [...queryKeys.season.all, "brawl-pass"] as const,
    soloRank: () => [...queryKeys.season.all, "solo-rank"] as const,
  },
  statistics: {
    all: ["statistics"] as const,
    brawlerResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "brawler-result", params] as const,
    brawlerRank: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "brawler-rank", params] as const,
    pairResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "pair-result", params] as const,
    pairRank: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "pair-rank", params] as const,
    enemyResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "enemy-result", params] as const,
    daily: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "daily", params] as const,
    itemOwnership: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "item-ownership", params] as const,
  },
} as const;
