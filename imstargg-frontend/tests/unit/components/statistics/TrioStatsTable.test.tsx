import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TrioStatsTable } from "@app/brawlers/[id]/_components/TrioStatsTable";
import type { Brawler } from "@/domain/brawler/types";
import type { TrioResultStat } from "@/domain/statistics/types";

const self: Brawler = { id: 16000000, name: "브롤러", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const shelly: Brawler = { id: 16000001, name: "쉘리", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const colt: Brawler = { id: 16000002, name: "콜트", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const bull: Brawler = { id: 16000003, name: "불", rarity: "EPIC", role: "TANK", portraitImageUrl: null };

function trio(pairBrawlers: Brawler[], tierScore = 60): TrioResultStat {
  return { brawler: self, pairBrawlers, battleCount: 100, pickRate: 0.1, winRate: 0.55, tierScore, tier: "S" };
}

describe("TrioStatsTable", () => {
  it("조합 브롤러 2명이 각자 상세 링크다", () => {
    render(<TrioStatsTable items={[trio([shelly, colt])]} />);
    expect(screen.getByRole("link", { name: "쉘리" })).toHaveAttribute("href", "/brawlers/16000001");
    expect(screen.getByRole("link", { name: "콜트" })).toHaveAttribute("href", "/brawlers/16000002");
  });

  it("pairBrawlers 가 2명이 아닌 행은 그리지 않는다", () => {
    render(<TrioStatsTable items={[trio([shelly, colt]), trio([bull], 50)]} />);
    expect(screen.queryByRole("link", { name: "불" })).not.toBeInTheDocument();
    expect(screen.getAllByRole("link")).toHaveLength(2);
  });
});
