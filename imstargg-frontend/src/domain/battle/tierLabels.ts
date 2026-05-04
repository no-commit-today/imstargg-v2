import type { SoloRankTier } from "@/api/types";

const TIER_BASE_COLOR: Record<string, string> = {
  BRONZE: "#C77545",
  SILVER: "#9FAFC5",
  GOLD: "#F2B93B",
  DIAMOND: "#58D7E8",
  MYTHIC: "#B83BFF",
  LEGENDARY: "#FF3A3A",
  MASTER: "#FFD447",
  PRO: "#FFD447",
};

const TIER_BASE_STICKER: Record<string, string> = {
  BRONZE: "/assets/icon_rank_sticker_bronze.webp",
  SILVER: "/assets/icon_rank_sticker_silver.webp",
  GOLD: "/assets/icon_rank_sticker_gold.webp",
  DIAMOND: "/assets/icon_rank_sticker_diamond.webp",
  MYTHIC: "/assets/icon_rank_sticker_mythic.webp",
  LEGENDARY: "/assets/icon_rank_sticker_legendary.webp",
  MASTER: "/assets/icon_rank_sticker_masters.webp",
  PRO: "/assets/icon_rank_sticker_pro.webp",
};

function tierBase(tier: SoloRankTier): string {
  if (tier === "PRO") return "PRO";
  return tier.split("_")[0];
}

export function tierColor(tier: SoloRankTier): string {
  return TIER_BASE_COLOR[tierBase(tier)];
}

export function tierStickerImage(tier: SoloRankTier): string {
  return TIER_BASE_STICKER[tierBase(tier)];
}

export function tierSegmentNumber(tier: SoloRankTier): string | null {
  if (tier === "PRO") return null;
  const seg = tier.split("_")[1];
  return seg ?? null;
}

/** @deprecated Task 14 에서 caller (BattlePlayerRow) 가 사라지고 Task 15 에서 이 alias 삭제됨 */
export function tierLabel(tier: SoloRankTier): string {
  return tierSegmentNumber(tier) ?? "";
}
