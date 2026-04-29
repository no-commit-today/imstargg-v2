import { describe, expect, it } from "vitest";
import {
  parseResultSortBy,
  parseRankSortBy,
  parseSortOrder,
} from "@/domain/statistics/sort";

describe("parseResultSortBy", () => {
  it("PICK_RATE 를 그대로 반환", () => {
    expect(parseResultSortBy("PICK_RATE")).toBe("PICK_RATE");
  });
  it("WIN_RATE 를 그대로 반환", () => {
    expect(parseResultSortBy("WIN_RATE")).toBe("WIN_RATE");
  });
  it("소문자도 정규화", () => {
    expect(parseResultSortBy("win_rate")).toBe("WIN_RATE");
  });
  it("알 수 없는 값은 PICK_RATE 로 fallback", () => {
    expect(parseResultSortBy("FOO")).toBe("PICK_RATE");
  });
  it("AVERAGE_RANK 는 결과형에 없으므로 PICK_RATE 로 fallback", () => {
    expect(parseResultSortBy("AVERAGE_RANK")).toBe("PICK_RATE");
  });
});

describe("parseRankSortBy", () => {
  it("AVERAGE_RANK 를 그대로 반환", () => {
    expect(parseRankSortBy("AVERAGE_RANK")).toBe("AVERAGE_RANK");
  });
  it("PICK_RATE 를 그대로 반환", () => {
    expect(parseRankSortBy("PICK_RATE")).toBe("PICK_RATE");
  });
  it("알 수 없는 값은 PICK_RATE 로 fallback", () => {
    expect(parseRankSortBy("FOO")).toBe("PICK_RATE");
  });
});

describe("parseSortOrder", () => {
  it("ASC 를 그대로 반환", () => {
    expect(parseSortOrder("ASC")).toBe("ASC");
  });
  it("desc 도 DESC 로 정규화", () => {
    expect(parseSortOrder("desc")).toBe("DESC");
  });
  it("알 수 없는 값은 DESC 로 fallback", () => {
    expect(parseSortOrder("FOO")).toBe("DESC");
  });
});
