import type { ClubType, ClubMemberRole } from "@/api/types";

export type ClubTypeDomain = ClubType; // alias kept for clarity at usage sites
export type ClubMemberRoleDomain = ClubMemberRole;

export interface ClubMember {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole;
  trophies: number;
  iconId: number;
}

export interface Club {
  tag: string;
  name: string;
  description: string;
  type: ClubType;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMember[];
}
