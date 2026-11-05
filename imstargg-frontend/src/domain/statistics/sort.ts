export const RESULT_SORT_BYS = ["PICK_RATE", "WIN_RATE"] as const;
export type ResultStatsSortBy = (typeof RESULT_SORT_BYS)[number];

export const COMBO_SORT_BYS = ["TIER_SCORE", "WIN_RATE", "PICK_RATE", "BATTLE_COUNT"] as const;
export type ComboStatsSortBy = (typeof COMBO_SORT_BYS)[number];

export const SORT_ORDERS = ["ASC", "DESC"] as const;
export type SortOrder = (typeof SORT_ORDERS)[number];

export const RESULT_SORT_LABELS: Record<ResultStatsSortBy, string> = {
  PICK_RATE: "픽률",
  WIN_RATE: "승률",
};

export const COMBO_SORT_LABELS: Record<ComboStatsSortBy, string> = {
  TIER_SCORE: "티어",
  WIN_RATE: "승률",
  PICK_RATE: "픽률",
  BATTLE_COUNT: "전투수",
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

export function parseSortOrder(value: unknown): SortOrder {
  return parseEnum(value, SORT_ORDERS, "DESC");
}

export function toggleSortOrder(order: SortOrder): SortOrder {
  return order === "ASC" ? "DESC" : "ASC";
}

function sortSign(order: SortOrder): number {
  return order === "DESC" ? -1 : 1;
}

export function sortResultStats<T extends { pickRate: number; winRate: number }>(
  items: T[],
  sortBy: ResultStatsSortBy,
  order: SortOrder,
): T[] {
  const sign = sortSign(order);
  return [...items].sort((a, b) => {
    switch (sortBy) {
      case "WIN_RATE":
        return (a.winRate - b.winRate) * sign;
      case "PICK_RATE":
      default:
        return (a.pickRate - b.pickRate) * sign;
    }
  });
}

export function sortComboStats<
  T extends { tierScore: number; winRate: number; pickRate: number; battleCount: number },
>(items: T[], sortBy: ComboStatsSortBy, order: SortOrder): T[] {
  const sign = sortSign(order);
  return [...items].sort((a, b) => {
    switch (sortBy) {
      case "WIN_RATE":
        return (a.winRate - b.winRate) * sign;
      case "PICK_RATE":
        return (a.pickRate - b.pickRate) * sign;
      case "BATTLE_COUNT":
        return (a.battleCount - b.battleCount) * sign;
      case "TIER_SCORE":
      default:
        return (a.tierScore - b.tierScore) * sign;
    }
  });
}
