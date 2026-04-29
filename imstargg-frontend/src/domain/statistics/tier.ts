export const STAT_TIERS = ["S", "A", "B", "C", "D"] as const;
export type StatTier = (typeof STAT_TIERS)[number];

export const TIER_COLOR: Record<StatTier, string> = {
  S: "#FFC60A",
  A: "#FF3A3A",
  B: "#FF8A1F",
  C: "#2BD576",
  D: "#5C6271",
};

export function parseTier(value: unknown): StatTier {
  if (typeof value !== "string") return "D";
  const normalized = value.trim().toUpperCase();
  return (STAT_TIERS as readonly string[]).includes(normalized) ? (normalized as StatTier) : "D";
}
