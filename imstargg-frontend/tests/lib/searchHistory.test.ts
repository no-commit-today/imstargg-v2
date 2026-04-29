import { describe, it, expect, beforeEach } from "vitest";
import { addRecent, getRecents, clearRecents, RECENTS_LIMIT } from "@/lib/searchHistory";

describe("searchHistory", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("초기 상태는 빈 배열", () => {
    expect(getRecents()).toEqual([]);
  });

  it("addRecent 후 getRecents 로 조회 가능", () => {
    addRecent({ kind: "player", tag: "#ABC" });
    const recents = getRecents();
    expect(recents).toHaveLength(1);
    expect(recents[0].tag).toBe("#ABC");
    expect(recents[0].kind).toBe("player");
    expect(typeof recents[0].addedAt).toBe("number");
  });

  it("최신 항목이 맨 앞", () => {
    addRecent({ kind: "player", tag: "#A" });
    addRecent({ kind: "player", tag: "#B" });
    expect(getRecents().map((r) => r.tag)).toEqual(["#B", "#A"]);
  });

  it("중복 태그(같은 kind+tag)는 가장 최근으로 끌어올림", () => {
    addRecent({ kind: "player", tag: "#A" });
    addRecent({ kind: "player", tag: "#B" });
    addRecent({ kind: "player", tag: "#A" });
    expect(getRecents().map((r) => r.tag)).toEqual(["#A", "#B"]);
  });

  it(`최대 ${RECENTS_LIMIT} 개까지만 유지 (FIFO)`, () => {
    for (let i = 0; i < RECENTS_LIMIT + 5; i++) {
      addRecent({ kind: "player", tag: `#T${i}` });
    }
    expect(getRecents()).toHaveLength(RECENTS_LIMIT);
    expect(getRecents()[0].tag).toBe(`#T${RECENTS_LIMIT + 4}`);
  });

  it("clearRecents 후 빈 배열", () => {
    addRecent({ kind: "player", tag: "#A" });
    clearRecents();
    expect(getRecents()).toEqual([]);
  });

  it("정규화: 입력 태그가 # 없어도 # 붙여서 저장", () => {
    addRecent({ kind: "player", tag: "abc" });
    expect(getRecents()[0].tag).toBe("#ABC");
  });

  it("storage 손상 시 빈 배열 + 키 삭제", () => {
    localStorage.setItem("imstargg.search.recents", "{not json");
    expect(getRecents()).toEqual([]);
    expect(localStorage.getItem("imstargg.search.recents")).toBeNull();
  });
});
