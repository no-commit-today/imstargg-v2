import { describe, expect, it } from "vitest";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  BRAWLER_TROPHY_RANGES,
  TIER_RANGE_LABELS,
  DATE_RANGE_LABELS,
  TROPHY_RANGE_LABELS,
  parseTierRange,
  parseDateRange,
  parseTrophyRange,
} from "@/domain/statistics/enums";

describe("STATS_TIER_RANGES", () => {
  it("6개 tier range 를 가진다", () => {
    expect(STATS_TIER_RANGES).toEqual([
      "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
      "SOLO_RANK_MYTHIC_PLUS", "SOLO_RANK_LEGENDARY_PLUS", "SOLO_RANK_MASTER_PLUS",
    ]);
  });
});

describe("STATS_DATE_RANGES", () => {
  it("8개 date range 를 가진다", () => {
    expect(STATS_DATE_RANGES).toHaveLength(8);
    expect(STATS_DATE_RANGES).toContain("SINCE_LAST_WEEK");
  });
});

describe("BRAWLER_TROPHY_RANGES", () => {
  it("4개 trophy range 를 가진다", () => {
    expect(BRAWLER_TROPHY_RANGES).toEqual([
      "TROPHY_500_PLUS", "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
    ]);
  });
});

describe("LABELS", () => {
  it("모든 tier range 에 한글 라벨이 있다", () => {
    for (const r of STATS_TIER_RANGES) {
      expect(TIER_RANGE_LABELS[r]).toBeTruthy();
    }
  });
  it("모든 date range 에 한글 라벨이 있다", () => {
    for (const r of STATS_DATE_RANGES) {
      expect(DATE_RANGE_LABELS[r]).toBeTruthy();
    }
  });
  it("모든 trophy range 에 한글 라벨이 있다", () => {
    for (const r of BRAWLER_TROPHY_RANGES) {
      expect(TROPHY_RANGE_LABELS[r]).toBeTruthy();
    }
  });
});

describe("parseTierRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseTierRange("TROPHY_2000_PLUS")).toBe("TROPHY_2000_PLUS");
  });
  it("대소문자가 섞여도 정규화한다", () => {
    expect(parseTierRange("solo_rank_mythic_plus")).toBe("SOLO_RANK_MYTHIC_PLUS");
  });
  it("알 수 없는 값은 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTierRange("FOO")).toBe("TROPHY_1000_PLUS");
  });
  it("undefined 는 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTierRange(undefined)).toBe("TROPHY_1000_PLUS");
  });
});

describe("parseDateRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseDateRange("LAST_WEEK")).toBe("LAST_WEEK");
  });
  it("알 수 없는 값은 SINCE_LAST_WEEK 로 fallback", () => {
    expect(parseDateRange("FOO")).toBe("SINCE_LAST_WEEK");
  });
  it("undefined 는 SINCE_LAST_WEEK 로 fallback", () => {
    expect(parseDateRange(undefined)).toBe("SINCE_LAST_WEEK");
  });
});

describe("parseTrophyRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseTrophyRange("TROPHY_500_PLUS")).toBe("TROPHY_500_PLUS");
  });
  it("알 수 없는 값은 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTrophyRange("FOO")).toBe("TROPHY_1000_PLUS");
  });
});
