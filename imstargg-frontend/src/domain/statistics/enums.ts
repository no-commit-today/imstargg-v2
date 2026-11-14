export const STATS_TIER_RANGES = [
  "TROPHY_1000_PLUS",
  "TROPHY_2000_PLUS",
  "TROPHY_3000_PLUS",
  "SOLO_RANK_MYTHIC_PLUS",
  "SOLO_RANK_LEGENDARY_PLUS",
  "SOLO_RANK_MASTER_PLUS",
] as const;
export type StatsTierRange = (typeof STATS_TIER_RANGES)[number];

export const STATS_DATE_RANGES = [
  "THIS_WEEK",
  "SINCE_LAST_WEEK",
  "LAST_WEEK",
  "SINCE_TWO_WEEKS_AGO",
  "TWO_WEEKS_AGO",
  "THIS_MONTH",
  "SINCE_LAST_MONTH",
  "LAST_MONTH",
] as const;
export type StatsDateRange = (typeof STATS_DATE_RANGES)[number];

export const BRAWLER_TROPHY_RANGES = [
  "TROPHY_500_PLUS",
  "TROPHY_1000_PLUS",
  "TROPHY_2000_PLUS",
  "TROPHY_3000_PLUS",
] as const;
export type BrawlerTrophyRange = (typeof BRAWLER_TROPHY_RANGES)[number];

export const TIER_RANGE_LABELS: Record<StatsTierRange, string> = {
  TROPHY_1000_PLUS: "트로피 1,000+",
  TROPHY_2000_PLUS: "트로피 2,000+",
  TROPHY_3000_PLUS: "트로피 3,000+",
  SOLO_RANK_MYTHIC_PLUS: "신화+",
  SOLO_RANK_LEGENDARY_PLUS: "전설+",
  SOLO_RANK_MASTER_PLUS: "마스터+",
};

export const DATE_RANGE_LABELS: Record<StatsDateRange, string> = {
  THIS_WEEK: "이번 주",
  SINCE_LAST_WEEK: "지난 주부터",
  LAST_WEEK: "지난 주",
  SINCE_TWO_WEEKS_AGO: "2주 전부터",
  TWO_WEEKS_AGO: "2주 전",
  THIS_MONTH: "이번 달",
  SINCE_LAST_MONTH: "지난 달부터",
  LAST_MONTH: "지난 달",
};

export const TROPHY_RANGE_LABELS: Record<BrawlerTrophyRange, string> = {
  TROPHY_500_PLUS: "500+",
  TROPHY_1000_PLUS: "1,000+",
  TROPHY_2000_PLUS: "2,000+",
  TROPHY_3000_PLUS: "3,000+",
};

/**
 * 티어 셀렉터 좌측에 표시할 아이콘 이미지 경로.
 * 트로피 구간은 icon_trophy.png, 경쟁전 구간은 해당 랭크 배지.
 */
export const TIER_RANGE_ICONS: Record<StatsTierRange, string> = {
  TROPHY_1000_PLUS: "/assets/icon_trophy.png",
  TROPHY_2000_PLUS: "/assets/icon_trophy.png",
  TROPHY_3000_PLUS: "/assets/icon_trophy.png",
  SOLO_RANK_MYTHIC_PLUS: "/assets/icon_rank_sticker_mythic.webp",
  SOLO_RANK_LEGENDARY_PLUS: "/assets/icon_rank_sticker_legendary.webp",
  SOLO_RANK_MASTER_PLUS: "/assets/icon_rank_sticker_masters.webp",
};

export const TROPHY_RANGE_ICON = "/assets/icon_trophy.png";

/** 브롤러 메타 페이지의 보기 탭. 쿼리스트링 값이 소문자라 enum 도 소문자다. */
export const BRAWLER_META_VIEWS = ["brawler", "pair", "trio"] as const;
export type BrawlerMetaView = (typeof BRAWLER_META_VIEWS)[number];

export const BRAWLER_META_VIEW_LABELS: Record<BrawlerMetaView, string> = {
  brawler: "브롤러",
  pair: "2인 조합",
  trio: "3인 조합",
};

export const DEFAULT_TIER_RANGE: StatsTierRange = "TROPHY_1000_PLUS";
export const DEFAULT_DATE_RANGE: StatsDateRange = "SINCE_LAST_WEEK";
export const DEFAULT_TROPHY_RANGE: BrawlerTrophyRange = "TROPHY_1000_PLUS";
export const DEFAULT_BRAWLER_META_VIEW: BrawlerMetaView = "brawler";

function parseEnum<T extends string>(
  value: unknown,
  values: readonly T[],
  fallback: T,
  normalize: (value: string) => string = (v) => v.toUpperCase(),
): T {
  if (typeof value !== "string") return fallback;
  const normalized = normalize(value.trim());
  return (values as readonly string[]).includes(normalized) ? (normalized as T) : fallback;
}

export function parseTierRange(value: unknown): StatsTierRange {
  return parseEnum(value, STATS_TIER_RANGES, DEFAULT_TIER_RANGE);
}

export function parseDateRange(value: unknown): StatsDateRange {
  return parseEnum(value, STATS_DATE_RANGES, DEFAULT_DATE_RANGE);
}

export function parseTrophyRange(value: unknown): BrawlerTrophyRange {
  return parseEnum(value, BRAWLER_TROPHY_RANGES, DEFAULT_TROPHY_RANGE);
}

export function parseBrawlerMetaView(value: unknown): BrawlerMetaView {
  return parseEnum(value, BRAWLER_META_VIEWS, DEFAULT_BRAWLER_META_VIEW, (v) => v.toLowerCase());
}
