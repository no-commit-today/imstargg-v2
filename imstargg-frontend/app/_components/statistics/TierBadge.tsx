import { TIER_COLOR, type StatTier } from "@/domain/statistics/tier";

export function TierBadge({ tier }: { tier: StatTier }) {
  const color = TIER_COLOR[tier];
  return (
    <span
      aria-label={`${tier} 티어`}
      className="tier-badge inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-extrabold text-[#14161B] tabular-nums"
      style={{
        backgroundColor: color,
        boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)",
      }}
    >
      {tier}
    </span>
  );
}
