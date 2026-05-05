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
  { label: "200K+",    min: 200000, max: null   },
  { label: "180–200K", min: 180000, max: 200000 },
  { label: "160–180K", min: 160000, max: 180000 },
  { label: "140–160K", min: 140000, max: 160000 },
  { label: "120–140K", min: 120000, max: 140000 },
  { label: "100–120K", min: 100000, max: 120000 },
  { label: "80–100K",  min: 80000,  max: 100000 },
  { label: "60–80K",   min: 60000,  max: 80000  },
  { label: "40–60K",   min: 40000,  max: 60000  },
  { label: "20–40K",   min: 20000,  max: 40000  },
  { label: "<20K",     min: 0,      max: 20000  },
];

export function trophyDistribution(members: ClubMember[]): TrophyBucket[] {
  return BUCKETS.map((b) => ({
    ...b,
    count: members.filter(
      (m) => m.trophies >= b.min && (b.max === null || m.trophies < b.max),
    ).length,
  }));
}
