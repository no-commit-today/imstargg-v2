import { describe, expect, it } from "vitest";
import { mergeSearchParams } from "@/lib/shallowSearch";

describe("mergeSearchParams", () => {
  it("값을 설정하면 ?key=value 문자열을 만든다", () => {
    expect(mergeSearchParams("", { tier: "TROPHY_2000_PLUS" })).toBe(
      "?tier=TROPHY_2000_PLUS",
    );
  });

  it("null 이면 해당 키를 제거한다", () => {
    expect(mergeSearchParams("?tier=TROPHY_2000_PLUS&date=LAST_WEEK", { tier: null })).toBe(
      "?date=LAST_WEEK",
    );
  });

  it("모든 키가 제거되면 빈 문자열을 반환한다 (물음표 없음)", () => {
    expect(mergeSearchParams("?tier=TROPHY_2000_PLUS", { tier: null })).toBe("");
  });

  it("관련 없는 기존 키는 보존한다", () => {
    expect(mergeSearchParams("?country=KR", { tier: "TROPHY_3000_PLUS" })).toBe(
      "?country=KR&tier=TROPHY_3000_PLUS",
    );
  });
});
