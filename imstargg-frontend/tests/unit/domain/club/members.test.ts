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
  it("buckets are 90k+, 80–90k, 70–80k, 60–70k, <60k in that order", () => {
    const result = trophyDistribution([]);
    expect(result.map((b) => b.label)).toEqual([
      "90k+", "80–90k", "70–80k", "60–70k", "<60k",
    ]);
    expect(result.every((b) => b.count === 0)).toBe(true);
  });

  it("counts boundary values: 60000→60–70k, 80000→80–90k, 90000→90k+", () => {
    const members = [
      m({ trophies: 59999 }),
      m({ trophies: 60000 }),
      m({ trophies: 79999 }),
      m({ trophies: 80000 }),
      m({ trophies: 89999 }),
      m({ trophies: 90000 }),
    ];
    const counts = Object.fromEntries(
      trophyDistribution(members).map((b) => [b.label, b.count]),
    );
    expect(counts["<60k"]).toBe(1);
    expect(counts["60–70k"]).toBe(1);
    expect(counts["70–80k"]).toBe(1);
    expect(counts["80–90k"]).toBe(2);
    expect(counts["90k+"]).toBe(1);
  });
});
