import { describe, it, expect, beforeEach } from "vitest";
import { getFavorites, isFavorite, toggleFavorite } from "@/lib/favorites";

describe("favorites", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("초기 상태는 빈 배열", () => {
    expect(getFavorites()).toEqual([]);
    expect(isFavorite("player", "#A")).toBe(false);
  });

  it("toggleFavorite 추가 → true 반환 + isFavorite 갱신", () => {
    const result = toggleFavorite({ kind: "player", tag: "#A", name: "a", trophies: 1 });
    expect(result).toBe(true);
    expect(isFavorite("player", "#A")).toBe(true);
    expect(getFavorites()).toHaveLength(1);
  });

  it("toggleFavorite 두 번째 호출 → false 반환 + 제거", () => {
    toggleFavorite({ kind: "player", tag: "#A", name: "a", trophies: 1 });
    const result = toggleFavorite({ kind: "player", tag: "#A", name: "a", trophies: 1 });
    expect(result).toBe(false);
    expect(isFavorite("player", "#A")).toBe(false);
    expect(getFavorites()).toEqual([]);
  });

  it("kind 가 다르면 별도 항목", () => {
    toggleFavorite({ kind: "player", tag: "#A", name: "a", trophies: 1 });
    toggleFavorite({ kind: "club", tag: "#A", name: "a", trophies: 100 });
    expect(getFavorites()).toHaveLength(2);
  });

  it("정규화: # 없이 입력해도 # 붙여 저장 + 비교 가능", () => {
    toggleFavorite({ kind: "player", tag: "abc", name: "a", trophies: 1 });
    expect(isFavorite("player", "#ABC")).toBe(true);
    expect(isFavorite("player", "abc")).toBe(true);
  });

  it("legacy 항목 (name/trophies 없음) 은 읽을 때 필터링", () => {
    localStorage.setItem(
      "imstargg.search.favorites.v2",
      JSON.stringify([{ kind: "player", tag: "#OLD", addedAt: Date.now() }]),
    );
    expect(getFavorites()).toEqual([]);
  });

  it("storage 손상 시 빈 배열 + 키 삭제", () => {
    localStorage.setItem("imstargg.search.favorites.v2", "{not json");
    expect(getFavorites()).toEqual([]);
    expect(localStorage.getItem("imstargg.search.favorites.v2")).toBeNull();
  });
});
