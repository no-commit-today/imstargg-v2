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
    const result = toggleFavorite({ kind: "player", tag: "#A" });
    expect(result).toBe(true);
    expect(isFavorite("player", "#A")).toBe(true);
    expect(getFavorites()).toHaveLength(1);
  });

  it("toggleFavorite 두 번째 호출 → false 반환 + 제거", () => {
    toggleFavorite({ kind: "player", tag: "#A" });
    const result = toggleFavorite({ kind: "player", tag: "#A" });
    expect(result).toBe(false);
    expect(isFavorite("player", "#A")).toBe(false);
    expect(getFavorites()).toEqual([]);
  });

  it("kind 가 다르면 별도 항목", () => {
    toggleFavorite({ kind: "player", tag: "#A" });
    toggleFavorite({ kind: "club", tag: "#A" });
    expect(getFavorites()).toHaveLength(2);
  });

  it("정규화: # 없이 입력해도 # 붙여 저장 + 비교 가능", () => {
    toggleFavorite({ kind: "player", tag: "abc" });
    expect(isFavorite("player", "#ABC")).toBe(true);
    expect(isFavorite("player", "abc")).toBe(true);
  });

  it("storage 손상 시 빈 배열 + 키 삭제", () => {
    localStorage.setItem("imstargg.search.favorites", "{not json");
    expect(getFavorites()).toEqual([]);
    expect(localStorage.getItem("imstargg.search.favorites")).toBeNull();
  });
});
