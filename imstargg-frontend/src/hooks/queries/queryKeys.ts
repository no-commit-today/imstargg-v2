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
} as const;
