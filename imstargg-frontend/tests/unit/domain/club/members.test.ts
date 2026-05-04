import { describe, it, expect } from "vitest";
import { sortMembers, trophyDistribution } from "@/domain/club/members";
import type { ClubMember } from "@/domain/club/types";

const m = (overrides: Partial<ClubMember> = {}): ClubMember => ({
  tag: "#M",
  name: "n",
  nameColor: null,
  role: "MEMBER",
  trophies: 10000,
  iconId: 0,
  ...overrides,
});

describe("sortMembers", () => {
  it("orders by role rank PRESIDENT > VICE_PRESIDENT > SENIOR > MEMBER > UNKNOWN", () => {
    const input = [
      m({ tag: "#1", role: "MEMBER" }),
      m({ tag: "#2", role: "PRESIDENT" }),
      m({ tag: "#3", role: "UNKNOWN" }),
      m({ tag: "#4", role: "SENIOR" }),
      m({ tag: "#5", role: "VICE_PRESIDENT" }),
    ];
    expect(sortMembers(input).map((x) => x.tag)).toEqual([
      "#2", "#5", "#4", "#1", "#3",
    ]);
  });

  it("breaks ties by trophies descending", () => {
    const input = [
      m({ tag: "#a", role: "MEMBER", trophies: 100 }),
      m({ tag: "#b", role: "MEMBER", trophies: 300 }),
      m({ tag: "#c", role: "MEMBER", trophies: 200 }),
    ];
    expect(sortMembers(input).map((x) => x.tag)).toEqual(["#b", "#c", "#a"]);
  });

  it("does not mutate the input array", () => {
    const input = [m({ tag: "#a", role: "MEMBER" }), m({ tag: "#b", role: "PRESIDENT" })];
    const original = input.map((x) => x.tag);
    sortMembers(input);
    expect(input.map((x) => x.tag)).toEqual(original);
  });
});

describe("trophyDistribution", () => {
  it("buckets are 11 buckets from 200K+ down to <20K", () => {
    const result = trophyDistribution([]);
    expect(result.map((b) => b.label)).toEqual([
      "200K+", "180–200K", "160–180K", "140–160K", "120–140K",
      "100–120K", "80–100K", "60–80K", "40–60K", "20–40K", "<20K",
    ]);
    expect(result.every((b) => b.count === 0)).toBe(true);
  });

  it("counts boundary values across new bucket boundaries", () => {
    const members = [
      m({ trophies: 19999 }),    // <20K
      m({ trophies: 20000 }),    // 20–40K
      m({ trophies: 39999 }),    // 20–40K
      m({ trophies: 40000 }),    // 40–60K
      m({ trophies: 199999 }),   // 180–200K
      m({ trophies: 200000 }),   // 200K+
      m({ trophies: 250000 }),   // 200K+
    ];
    const counts = Object.fromEntries(
      trophyDistribution(members).map((b) => [b.label, b.count]),
    );
    expect(counts["<20K"]).toBe(1);
    expect(counts["20–40K"]).toBe(2);
    expect(counts["40–60K"]).toBe(1);
    expect(counts["180–200K"]).toBe(1);
    expect(counts["200K+"]).toBe(2);
  });
});
