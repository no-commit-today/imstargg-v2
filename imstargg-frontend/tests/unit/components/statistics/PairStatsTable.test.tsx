import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PairStatsTable } from "@app/brawlers/[id]/_components/PairStatsTable";
import type { Brawler } from "@/domain/brawler/types";
import type { PairResultStat } from "@/domain/statistics/types";

const self: Brawler = { id: 16000000, name: "브롤러", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const shelly: Brawler = { id: 16000001, name: "쉘리", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const colt: Brawler = { id: 16000002, name: "콜트", rarity: "EPIC", role: "TANK", portraitImageUrl: null };

function pair(pairBrawler: Brawler, tierScore: number, tier: PairResultStat["tier"]): PairResultStat {
  return { brawler: self, pairBrawler, battleCount: 100, pickRate: 0.1, winRate: 0.55, tierScore, tier };
}

describe("PairStatsTable", () => {
  it("티어 배지를 그린다", () => {
    render(<PairStatsTable items={[pair(shelly, 60, "S")]} />);
    expect(screen.getByLabelText("S 티어")).toBeInTheDocument();
  });

  it("픽률은 소수 둘째 자리까지 보인다", () => {
    render(<PairStatsTable items={[{ ...pair(shelly, 60, "S"), pickRate: 0.0034 }]} />);
    expect(screen.getByText("0.34%")).toBeInTheDocument();
  });

  it("기본 정렬은 tierScore 내림차순", () => {
    render(<PairStatsTable items={[pair(shelly, 40, "B"), pair(colt, 60, "S")]} />);
    const rows = screen.getAllByRole("link");
    expect(rows[0]).toHaveAccessibleName(/콜트/);
    expect(rows[1]).toHaveAccessibleName(/쉘리/);
  });
});
