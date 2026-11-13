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
  createdAt: string | null;
}

export type StatsEventModeType = "RESULT_3VS3" | "SOLO_SHOWDOWN" | "DUO_SHOWDOWN" | "TRIO_SHOWDOWN";

export const STATS_EVENT_MODE_TYPE_LABELS: Record<StatsEventModeType, string> = {
  RESULT_3VS3: "3대3 결과형 (2팀 × 3명)",
  SOLO_SHOWDOWN: "솔로 쇼다운 (10팀 × 1명)",
  DUO_SHOWDOWN: "듀오 쇼다운 (5팀 × 2명)",
  TRIO_SHOWDOWN: "트리오 쇼다운 (4팀 × 3명)",
};

export interface BattleEventMode {
  brawlStarsId: number;
  brawlStarsName: string;
  names: Record<Language, string>;
  iconImageUrl: string | null;
  // 통계 수집 유형. 통계를 모으지 않는 모드는 null
  statsType: StatsEventModeType | null;
}

export interface Maintenance {
  id: number;
  startTime: string;
  endTime: string;
  createdAt: string | null;
}

export type FeedbackRating =
  | "VERY_SATISFIED"
  | "SATISFIED"
  | "NEUTRAL"
  | "DISSATISFIED"
  | "VERY_DISSATISFIED";

export interface FeedbackItem {
  id: number;
  rating: FeedbackRating;
  content: string;
  checked: boolean;
  submittedAt: string;
}

export interface FeedbackListResponse {
  content: FeedbackItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FeedbackSearchParams {
  rating?: FeedbackRating;
  checked?: boolean;
  page?: number;
  size?: number;
}

export interface ApiSpecListItem {
  id: number;
  collectedAt: string;
  sizeBytes: number;
  lineCount: number;
}

export interface ApiSpecDetail {
  id: number;
  collectedAt: string;
  content: string;
  previousId: number | null;
  previousCollectedAt: string | null;
  previousContent: string | null;
}

export interface BrawlStarsPatch {
  id: number;
  patchDate: string;
  memo: string | null;
}

export interface BrawlStarsPatchList {
  today: string;
  earliestRegistrableDate: string;
  patches: BrawlStarsPatch[];
}

export type NewsArticleType = "RELEASE_NOTE" | "OTHER";

export interface NewsArticleListItem {
  id: number;
  articleKey: string;
  linkUrl: string;
  title: string;
  type: NewsArticleType;
  publishedAt: string;
  seenAt: string;
  versionCount: number;
}

export interface NewsArticleDetail {
  id: number;
  articleKey: string;
  linkUrl: string;
  title: string;
  type: NewsArticleType;
  publishedAt: string;
  seenAt: string;
  content: string;
  previousId: number | null;
  previousSeenAt: string | null;
  previousContent: string | null;
}

export interface NewsArticleVersion {
  id: number;
  linkUrl: string;
  title: string;
  publishedAt: string;
  seenAt: string;
}
