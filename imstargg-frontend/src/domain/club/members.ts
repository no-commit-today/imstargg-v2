import type { ClubMember } from "@/domain/club/types";
import type { ClubMemberRole } from "@/api/types";

const ROLE_RANK: Record<ClubMemberRole, number> = {
  PRESIDENT: 0,
  VICE_PRESIDENT: 1,
  SENIOR: 2,
  MEMBER: 3,
  UNKNOWN: 4,
};

export function sortMembers(members: ClubMember[]): ClubMember[] {
  return [...members].sort((a, b) => {
    const roleDiff = ROLE_RANK[a.role] - ROLE_RANK[b.role];
    if (roleDiff !== 0) return roleDiff;
    return b.trophies - a.trophies;
  });
}

export interface TrophyBucket {
  label: string;
  min: number;
  max: number | null; // exclusive upper bound; null = open
  count: number;
}

const BUCKETS: ReadonlyArray<Omit<TrophyBucket, "count">> = [
  { label: "90k+", min: 90000, max: null },
  { label: "80–90k", min: 80000, max: 90000 },
  { label: "70–80k", min: 70000, max: 80000 },
  { label: "60–70k", min: 60000, max: 70000 },
  { label: "<60k", min: 0, max: 60000 },
];

export function trophyDistribution(members: ClubMember[]): TrophyBucket[] {
  return BUCKETS.map((b) => ({
    ...b,
    count: members.filter(
      (m) => m.trophies >= b.min && (b.max === null || m.trophies < b.max),
    ).length,
  }));
}
