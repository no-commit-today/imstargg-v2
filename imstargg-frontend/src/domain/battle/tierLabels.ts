import type { SoloRankTier } from "@/api/types";

const TIER_LABEL_PREFIX: Record<string, string> = {
  BRONZE: "B",
  SILVER: "S",
  GOLD: "G",
  DIAMOND: "D",
  MYTHIC: "M",
  LEGENDARY: "L",
};

const TIER_COLOR: Record<SoloRankTier, string> = {
  BRONZE_1: "#C77545",
  BRONZE_2: "#C77545",
  BRONZE_3: "#C77545",
  SILVER_1: "#9FAFC5",
  SILVER_2: "#9FAFC5",
  SILVER_3: "#9FAFC5",
  GOLD_1: "#F2B93B",
  GOLD_2: "#F2B93B",
  GOLD_3: "#F2B93B",
  DIAMOND_1: "#58D7E8",
  DIAMOND_2: "#58D7E8",
  DIAMOND_3: "#58D7E8",
  MYTHIC_1: "#B83BFF",
  MYTHIC_2: "#B83BFF",
  MYTHIC_3: "#B83BFF",
  LEGENDARY_1: "#FF3A3A",
  LEGENDARY_2: "#FF3A3A",
  LEGENDARY_3: "#FF3A3A",
  MASTERS: "#FFD447",
};

export function tierLabel(tier: SoloRankTier): string {
  if (tier === "MASTERS") return "MS";
  const [base, num] = tier.split("_");
  const prefix = TIER_LABEL_PREFIX[base] ?? base.slice(0, 1);
  return `${prefix}${num ?? ""}`;
}

export function tierColor(tier: SoloRankTier): string {
  return TIER_COLOR[tier];
}
