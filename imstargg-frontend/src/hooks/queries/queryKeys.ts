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
} as const;
