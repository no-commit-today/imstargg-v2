export interface DashboardSummary {
  brawlerTotal: number;
  brawlerWithName: number;
  brawlerWithImage: number;
  gearTotal: number;
  gearWithName: number;
  gearWithImage: number;
  eventModeTotal: number;
  eventModeWithName: number;
  eventModeWithIcon: number;
  eventMapTotal: number;
  eventMapWithName: number;
}

export type Language = "KOREAN" | "ENGLISH";

export interface BrawlerListItem {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  role: string;
  imageUrl: string | null;
}

export interface StarPower {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface Gadget {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface HyperCharge {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface Gear {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  imageUrl: string | null;
}

export type BuffyType = "GADGET" | "STAR_POWER" | "HYPER_CHARGE";

export interface Buffy {
  type: BuffyType;
  imageUrl: string | null;
}

export interface BrawlerDetail {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  role: string;
  imageUrl: string | null;
  buffies: Buffy[];
  starPowers: StarPower[];
  gadgets: Gadget[];
  hyperCharges: HyperCharge[];
  linkedGearBrawlStarsIds: number[];
  allGears: Gear[];
}

export interface BattleEvent {
  brawlStarsId: number;
  modeBrawlStarsName: string | null;
  mapName: string | null;
  mapNames: Record<Language, string>;
}

export type BattleEventModeResultType = "RESULT" | "RANK";

export const BATTLE_EVENT_MODE_RESULT_TYPES: BattleEventModeResultType[] = ["RESULT", "RANK"];

export interface BattleEventMode {
  brawlStarsId: number;
  brawlStarsName: string;
  names: Record<Language, string>;
  iconImageUrl: string | null;
  resultType: BattleEventModeResultType;
}

export interface Maintenance {
  id: number;
  startTime: string;
  endTime: string;
  createdAt: string | null;
}

export type FeedbackCategory = "BUG" | "FEATURE_REQUEST" | "ETC";

export interface FeedbackItem {
  id: number;
  category: FeedbackCategory;
  content: string;
  email: string | null;
  checked: boolean;
  createdAt: string;
}

export interface FeedbackListResponse {
  content: FeedbackItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FeedbackSearchParams {
  category?: FeedbackCategory;
  checked?: boolean;
  page?: number;
  size?: number;
}
