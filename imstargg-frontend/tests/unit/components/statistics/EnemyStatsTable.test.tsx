import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EnemyStatsTable } from "@app/brawlers/[id]/_components/EnemyStatsTable";
import type { Brawler } from "@/domain/brawler/types";
import type { EnemyResultStat } from "@/domain/statistics/types";

const self: Brawler = { id: 16000000, name: "브롤러", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const shelly: Brawler = { id: 16000001, name: "쉘리", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const colt: Brawler = { id: 16000002, name: "콜트", rarity: "EPIC", role: "TANK", portraitImageUrl: null };

function enemy(enemyBrawler: Brawler, winRate: number): EnemyResultStat {
  return { brawler: self, enemyBrawler, battleCount: 100, pickRate: 0.1, winRate };
}

describe("EnemyStatsTable", () => {
  it("부제는 '이 상대를 만났을 때 내 승률' 이다", () => {
    render(<EnemyStatsTable items={[enemy(shelly, 0.6)]} />);
    expect(screen.getByText(/이 상대를 만났을 때 내 승률/)).toBeInTheDocument();
  });

  it("승률 0.6 행은 data-rail=win, 0.4 는 loss", () => {
    render(<EnemyStatsTable items={[enemy(shelly, 0.6), enemy(colt, 0.4)]} />);
    expect(screen.getByRole("link", { name: /쉘리/ }).getAttribute("data-rail")).toBe("win");
    expect(screen.getByRole("link", { name: /콜트/ }).getAttribute("data-rail")).toBe("loss");
  });

  it("기본 정렬은 승률 오름차순", () => {
    render(<EnemyStatsTable items={[enemy(shelly, 0.6), enemy(colt, 0.4)]} />);
    const rows = screen.getAllByRole("link");
    expect(rows[0]).toHaveAccessibleName(/콜트/);
    expect(rows[1]).toHaveAccessibleName(/쉘리/);
  });
});
