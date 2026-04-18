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

export interface BrawlerDetail {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  role: string;
  imageUrl: string | null;
  buffyImages: Record<string, string | null>;
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

export interface BattleEventMode {
  brawlStarsId: number;
  brawlStarsName: string;
  names: Record<Language, string>;
  iconImageUrl: string | null;
}

export interface Maintenance {
  id: number;
  startTime: string;
  endTime: string;
  createdAt: string | null;
}
