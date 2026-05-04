import { describe, expect, it } from "vitest";
import { sortPlayerBrawlers } from "@/domain/player/sortPlayerBrawlers";
import type { PlayerBrawler } from "@/domain/player/types";

function pb(overrides: Partial<PlayerBrawler> & { id?: number }): PlayerBrawler {
  const { id = 0, ...rest } = overrides;
  return {
    brawler: rest.brawler ?? {
      id,
      name: `Brawler ${id}`,
      rarity: "RARE",
      role: "DAMAGE_DEALER",
      portraitImageUrl: null,
    },
    power: 1,
    rank: 1,
    trophies: 0,
    highestTrophies: 0,
    prestigeLevel: 0,
    gadgets: [],
    gears: [],
    starPowers: [],
    hyperCharge: null,
    buffies: null,
    ...rest,
  } as PlayerBrawler;
}

describe("sortPlayerBrawlers", () => {
  it("trophies desc 로 정렬한다", () => {
    const list = [pb({ id: 1, trophies: 100 }), pb({ id: 2, trophies: 500 })];
    const sorted = sortPlayerBrawlers(list, "trophies", "desc");
    expect(sorted.map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("trophies asc 로 정렬한다", () => {
    const list = [pb({ id: 1, trophies: 500 }), pb({ id: 2, trophies: 100 })];
    expect(sortPlayerBrawlers(list, "trophies", "asc").map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("희귀도 순으로 정렬한다", () => {
    const list = [
      pb({ id: 1, brawler: { id: 1, name: "a", rarity: "LEGENDARY", role: "TANK", portraitImageUrl: null } }),
      pb({ id: 2, brawler: { id: 2, name: "b", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
    ];
    expect(sortPlayerBrawlers(list, "rarity", "desc").map((p) => p.brawler!.id)).toEqual([1, 2]);
  });

  it("이름 asc 로 정렬한다", () => {
    const list = [
      pb({ id: 1, brawler: { id: 1, name: "쉘리", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
      pb({ id: 2, brawler: { id: 2, name: "콜트", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
    ];
    expect(sortPlayerBrawlers(list, "name", "asc").map((p) => p.brawler!.id)).toEqual([1, 2]);
  });

  it("최신순 (id desc) 로 정렬한다", () => {
    const list = [pb({ id: 1 }), pb({ id: 2 })];
    expect(sortPlayerBrawlers(list, "id", "desc").map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("brawler 가 null 인 항목도 안전하게 처리한다", () => {
    const list = [pb({ id: 1, trophies: 100 }), { ...pb({ id: 2 }), brawler: null }];
    expect(() => sortPlayerBrawlers(list, "name", "asc")).not.toThrow();
  });
});
