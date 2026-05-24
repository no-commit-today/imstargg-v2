import { describe, expect, it } from "vitest";
import {
  parseResultSortBy,
  parseRankSortBy,
  parseSortOrder,
  sortRankStats,
  sortResultStats,
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

describe("sortResultStats", () => {
  const items = [
    { id: "a", pickRate: 0.1, winRate: 0.7 },
    { id: "b", pickRate: 0.3, winRate: 0.5 },
    { id: "c", pickRate: 0.2, winRate: 0.9 },
  ];

  it("PICK_RATE 기준으로 정렬한다", () => {
    expect(sortResultStats(items, "PICK_RATE", "DESC").map((item) => item.id)).toEqual([
      "b",
      "c",
      "a",
    ]);
  });

  it("WIN_RATE 기준으로 정렬한다", () => {
    expect(sortResultStats(items, "WIN_RATE", "ASC").map((item) => item.id)).toEqual([
      "b",
      "a",
      "c",
    ]);
  });

  it("입력 배열을 변경하지 않는다", () => {
    sortResultStats(items, "PICK_RATE", "DESC");

    expect(items.map((item) => item.id)).toEqual(["a", "b", "c"]);
  });
});

describe("sortRankStats", () => {
  const items = [
    { id: "a", averageRank: 2.2, totalBattleCount: 100 },
    { id: "b", averageRank: 1.8, totalBattleCount: 250 },
    { id: "c", averageRank: 3.1, totalBattleCount: 150 },
  ];

  it("AVERAGE_RANK 기준으로 정렬한다", () => {
    expect(sortRankStats(items, "AVERAGE_RANK", "ASC").map((item) => item.id)).toEqual([
      "b",
      "a",
      "c",
    ]);
  });

  it("PICK_RATE 는 전투수 기준으로 정렬한다", () => {
    expect(sortRankStats(items, "PICK_RATE", "DESC").map((item) => item.id)).toEqual([
      "b",
      "c",
      "a",
    ]);
  });

  it("입력 배열을 변경하지 않는다", () => {
    sortRankStats(items, "PICK_RATE", "DESC");

    expect(items.map((item) => item.id)).toEqual(["a", "b", "c"]);
  });
});
