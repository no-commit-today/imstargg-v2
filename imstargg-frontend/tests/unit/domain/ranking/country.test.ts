import { describe, expect, it } from "vitest";
import { COUNTRY_CODES, COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";

describe("COUNTRY_CODES", () => {
  it("6개 country code를 가진다", () => {
    expect(COUNTRY_CODES).toEqual(["global", "kr", "cn", "jp", "de", "us"]);
  });
});

describe("COUNTRY_LABELS", () => {
  it("각 country code에 한글 라벨이 매핑된다", () => {
    expect(COUNTRY_LABELS.global).toBe("글로벌");
    expect(COUNTRY_LABELS.kr).toBe("한국");
    expect(COUNTRY_LABELS.cn).toBe("중국");
    expect(COUNTRY_LABELS.jp).toBe("일본");
    expect(COUNTRY_LABELS.de).toBe("독일");
    expect(COUNTRY_LABELS.us).toBe("미국");
  });
});

describe("parseCountry", () => {
  it("유효한 code는 그대로 반환한다", () => {
    expect(parseCountry("kr")).toBe("kr");
    expect(parseCountry("global")).toBe("global");
  });

  it("대문자/공백을 정규화한다", () => {
    expect(parseCountry(" KR ")).toBe("kr");
    expect(parseCountry("US")).toBe("us");
  });

  it("undefined/null/빈 문자열은 global로 fallback", () => {
    expect(parseCountry(undefined)).toBe("global");
    expect(parseCountry(null)).toBe("global");
    expect(parseCountry("")).toBe("global");
  });

  it("알 수 없는 code는 global로 fallback", () => {
    expect(parseCountry("fr")).toBe("global");
    expect(parseCountry("xyz")).toBe("global");
  });

  it("문자열이 아닌 입력은 global로 fallback", () => {
    expect(parseCountry(123)).toBe("global");
    expect(parseCountry(["kr"])).toBe("global");
  });
});
