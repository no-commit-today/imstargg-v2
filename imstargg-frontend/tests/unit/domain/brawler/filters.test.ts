import { describe, it, expect } from "vitest";
import { filterBrawlers, sortByName } from "@/domain/brawler/filters";
import type { Brawler } from "@/domain/brawler/types";

const b = (overrides: Partial<Brawler> = {}): Brawler => ({
  id: 1,
  name: "Shelly",
  rarity: "STARTING_BRAWLER",
  role: "DAMAGE_DEALER",
  portraitImageUrl: null,
  ...overrides,
});

describe("filterBrawlers", () => {
  const list: Brawler[] = [
    b({ id: 1, name: "셸리", role: "DAMAGE_DEALER", rarity: "STARTING_BRAWLER" }),
    b({ id: 2, name: "Shelly", role: "DAMAGE_DEALER", rarity: "STARTING_BRAWLER" }),
    b({ id: 3, name: "콜트", role: "MARKSMAN", rarity: "STARTING_BRAWLER" }),
    b({ id: 4, name: "리언", role: "ASSASSIN", rarity: "LEGENDARY" }),
  ];

  it("returns all when no opts", () => {
    expect(filterBrawlers(list, {})).toHaveLength(4);
  });

  it("filters by role", () => {
    const r = filterBrawlers(list, { role: "MARKSMAN" });
    expect(r.map((x) => x.id)).toEqual([3]);
  });

  it("filters by rarity", () => {
    const r = filterBrawlers(list, { rarity: "LEGENDARY" });
    expect(r.map((x) => x.id)).toEqual([4]);
  });

  it("substring search matches Korean", () => {
    const r = filterBrawlers(list, { search: "셸" });
    expect(r.map((x) => x.id)).toEqual([1]);
  });

  it("substring search matches English (case-insensitive)", () => {
    const r = filterBrawlers(list, { search: "shel" });
    expect(r.map((x) => x.id)).toEqual([2]);
  });

  it("combines role + search", () => {
    const r = filterBrawlers(list, { role: "DAMAGE_DEALER", search: "Shelly" });
    expect(r.map((x) => x.id)).toEqual([2]);
  });

  it("trims and ignores empty search", () => {
    expect(filterBrawlers(list, { search: "   " })).toHaveLength(4);
  });
});

describe("sortByName", () => {
  it("sorts Korean names by Korean collation", () => {
    const list: Brawler[] = [
      b({ id: 1, name: "콜트" }),
      b({ id: 2, name: "셸리" }),
      b({ id: 3, name: "리언" }),
    ];
    expect(sortByName(list).map((x) => x.name)).toEqual(["리언", "셸리", "콜트"]);
  });

  it("does not mutate input", () => {
    const list: Brawler[] = [b({ id: 1, name: "콜트" }), b({ id: 2, name: "셸리" })];
    const before = list.map((x) => x.id);
    sortByName(list);
    expect(list.map((x) => x.id)).toEqual(before);
  });
});
