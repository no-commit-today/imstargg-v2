import { describe, expect, it } from "vitest";
import { STAT_TIERS, TIER_COLOR, parseTier } from "@/domain/statistics/tier";

describe("STAT_TIERS", () => {
  it("S/A/B/C/D 5단계", () => {
    expect(STAT_TIERS).toEqual(["S", "A", "B", "C", "D"]);
  });
});

describe("TIER_COLOR", () => {
  it("S 는 브랜드 옐로우", () => {
    expect(TIER_COLOR.S).toBe("#FFC60A");
  });
  it("모든 tier 에 컬러가 있다", () => {
    for (const t of STAT_TIERS) {
      expect(TIER_COLOR[t]).toMatch(/^#[0-9A-F]{6}$/i);
    }
  });
});

describe("parseTier", () => {
  it("S/A/B/C/D 를 그대로 반환", () => {
    expect(parseTier("S")).toBe("S");
    expect(parseTier("A")).toBe("A");
    expect(parseTier("D")).toBe("D");
  });
  it("소문자도 정규화", () => {
    expect(parseTier("b")).toBe("B");
  });
  it("알 수 없는 값은 D 로 fallback", () => {
    expect(parseTier("F")).toBe("D");
    expect(parseTier("foo")).toBe("D");
    expect(parseTier(undefined)).toBe("D");
  });
});
