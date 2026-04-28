// 모든 타입은 백엔드 com.imstargg.core.api.controller.v1.response.* DTO와 동일.
// 백엔드 변경 시 이 파일과 매퍼만 수정한다.

export interface ListResponse<T> {
  content: T[];
}

// === Player ===
export interface PlayerResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  qualifiedFromChampionshipChallenge: boolean;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  bestRoboRumbleTime: number;
  bestTimeAsBigBrawler: number;
  totalPrestigeLevel: number;
  club: PlayerClubResponse | null;
}

export interface PlayerClubResponse {
  tag: string;
  name: string;
}

// === Brawler ===
export type BrawlerRarity =
  | "COMMON" | "RARE" | "SUPER_RARE" | "EPIC"
  | "MYTHIC" | "LEGENDARY" | "ULTRA_LEGENDARY";

export type BrawlerRole =
  | "DAMAGE_DEALER" | "TANK" | "MARKSMAN" | "ASSASSIN"
  | "SUPPORT" | "CONTROLLER" | "ARTILLERY";

export type BuffyType = "MYTHIC_GEAR" | string;

export interface BuffyResponse {
  type: BuffyType;
  imageUrl: string | null;
}

export type Language = "ko" | "en" | string;
export type MessagesResponse = Record<Language, string>;

export interface GadgetResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface GearResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface StarPowerResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface HyperChargeResponse { id: number; name: MessagesResponse; imageUrl: string | null; }

export interface BrawlerResponse {
  id: number;
  name: MessagesResponse;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  gadgets: GadgetResponse[];
  gears: GearResponse[];
  starPowers: StarPowerResponse[];
  hyperCharges: HyperChargeResponse[];
  portraitImageUrl: string | null;
  buffies: BuffyResponse[];
}

export interface PlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  buffies: BuffyResponse[];
}

// === Renewal ===
export type RenewalStatus =
  | "REQUESTED" | "PENDING" | "EXECUTING"
  | "COMPLETED" | "NOT_FOUND" | "FAILED";

export interface RenewalResponse {
  tag: string;
  status: RenewalStatus;
  progressing: boolean;
  canRenew: boolean;
  requestedAt: string;
  executedAt: string | null;
  completedAt: string | null;
  renewedAt: string | null;
}

// === Club ===
export type ClubType = "OPEN" | "INVITE_ONLY" | "CLOSED" | "UNKNOWN";

export type ClubMemberRole =
  | "PRESIDENT"
  | "VICE_PRESIDENT"
  | "SENIOR"
  | "MEMBER"
  | "UNKNOWN";

export interface ClubMemberResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole | null;
  trophies: number;
  iconId: number;
}

export interface ClubResponse {
  tag: string;
  name: string;
  description: string;
  type: ClubType | null;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMemberResponse[];
}
