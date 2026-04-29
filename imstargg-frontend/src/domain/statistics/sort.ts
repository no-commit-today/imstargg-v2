export const RESULT_SORT_BYS = ["PICK_RATE", "WIN_RATE"] as const;
export type ResultStatsSortBy = (typeof RESULT_SORT_BYS)[number];

export const RANK_SORT_BYS = ["PICK_RATE", "AVERAGE_RANK"] as const;
export type RankStatsSortBy = (typeof RANK_SORT_BYS)[number];

export const SORT_ORDERS = ["ASC", "DESC"] as const;
export type SortOrder = (typeof SORT_ORDERS)[number];

export const RESULT_SORT_LABELS: Record<ResultStatsSortBy, string> = {
  PICK_RATE: "픽률",
  WIN_RATE: "승률",
};

export const RANK_SORT_LABELS: Record<RankStatsSortBy, string> = {
  PICK_RATE: "픽률",
  AVERAGE_RANK: "평균 순위",
};

function parseEnum<T extends string>(
  value: unknown,
  values: readonly T[],
  fallback: T,
): T {
  if (typeof value !== "string") return fallback;
  const normalized = value.trim().toUpperCase();
  return (values as readonly string[]).includes(normalized) ? (normalized as T) : fallback;
}

export function parseResultSortBy(value: unknown): ResultStatsSortBy {
  return parseEnum(value, RESULT_SORT_BYS, "PICK_RATE");
}

export function parseRankSortBy(value: unknown): RankStatsSortBy {
  return parseEnum(value, RANK_SORT_BYS, "PICK_RATE");
}

export function parseSortOrder(value: unknown): SortOrder {
  return parseEnum(value, SORT_ORDERS, "DESC");
}

export function toggleSortOrder(order: SortOrder): SortOrder {
  return order === "ASC" ? "DESC" : "ASC";
}
