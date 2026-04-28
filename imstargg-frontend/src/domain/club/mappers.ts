import type { ClubResponse, ClubMemberResponse } from "@/api/types";
import type { Club, ClubMember } from "@/domain/club/types";
import { normalizeTag } from "@/lib/tag";

export function toClubMember(dto: ClubMemberResponse): ClubMember {
  return {
    tag: normalizeTag(dto.tag),
    name: dto.name,
    nameColor: dto.nameColor,
    role: dto.role ?? "UNKNOWN",
    trophies: dto.trophies,
    iconId: dto.iconId,
  };
}

export function toClub(dto: ClubResponse): Club {
  return {
    tag: normalizeTag(dto.tag),
    name: dto.name,
    description: dto.description,
    type: dto.type ?? "UNKNOWN",
    badgeId: dto.badgeId,
    requiredTrophies: dto.requiredTrophies,
    trophies: dto.trophies,
    members: dto.members.map(toClubMember),
  };
}
