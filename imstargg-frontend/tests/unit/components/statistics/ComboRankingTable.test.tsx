import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ComboRankingTable } from "@app/brawlers/_components/ComboRankingTable";
import type { Brawler } from "@/domain/brawler/types";
import type { ComboRankingStat } from "@/domain/statistics/types";

function brawler(id: number, name: string): Brawler {
  return { id, name, rarity: "EPIC", role: "TANK", portraitImageUrl: null };
}

const shelly = brawler(16000001, "쉘리");
const colt = brawler(16000002, "콜트");
const bull = brawler(16000003, "불");
const jessie = brawler(16000004, "제시");
const nita = brawler(16000005, "니타");

function combo(brawlers: Brawler[], overrides: Partial<ComboRankingStat> = {}): ComboRankingStat {
  return {
    brawlers,
    battleCount: 200,
    pickRate: 0.1,
    winRate: 0.55,
    tierScore: 60,
    tier: "S",
    ...overrides,
  };
}

describe("ComboRankingTable", () => {
  it("3인 조합 행은 아바타 3개와 상세 링크 3개", () => {
    const { container } = render(<ComboRankingTable items={[combo([shelly, colt, bull])]} />);

    const links = screen.getAllByRole("link");
    expect(links).toHaveLength(3);
    expect(screen.getByRole("link", { name: "쉘리" })).toHaveAttribute("href", "/brawlers/16000001");
    expect(screen.getByRole("link", { name: "콜트" })).toHaveAttribute("href", "/brawlers/16000002");
    expect(screen.getByRole("link", { name: "불" })).toHaveAttribute("href", "/brawlers/16000003");
    for (const link of links) {
      expect(within(link).getByTitle(link.getAttribute("aria-label") as string)).toBeInTheDocument();
    }
    expect(container.querySelectorAll("a [title]")).toHaveLength(3);
  });

  it("2인 조합 행은 상세 링크 2개", () => {
    render(<ComboRankingTable items={[combo([shelly, colt])]} />);
    expect(screen.getAllByRole("link")).toHaveLength(2);
  });

  it("브롤러가 2~3명이 아닌 행은 그리지 않는다", () => {
    render(
      <ComboRankingTable items={[combo([shelly, colt]), combo([bull], { tierScore: 50 })]} />,
    );
    expect(screen.queryByRole("link", { name: "불" })).not.toBeInTheDocument();
    expect(screen.getAllByRole("link")).toHaveLength(2);
  });

  it("빈 목록 문구", () => {
    render(<ComboRankingTable items={[]} />);
    expect(screen.getByText("전투 100회 이상인 조합이 없습니다")).toBeInTheDocument();
  });

  it("기본 정렬은 티어 점수 내림차순이다", () => {
    render(
      <ComboRankingTable
        items={[
          combo([shelly, colt], { tierScore: 40 }),
          combo([bull, jessie], { tierScore: 70 }),
        ]}
      />,
    );
    expect(screen.getAllByRole("link")[0]).toHaveAttribute("href", "/brawlers/16000003");
  });

  it("승률 헤더 클릭 시 승률 내림차순", async () => {
    render(
      <ComboRankingTable
        items={[
          // 티어 점수는 높지만 승률이 낮은 조합이 기본 정렬에서는 먼저 온다
          combo([shelly, colt], { winRate: 0.4, tierScore: 80 }),
          combo([bull, jessie], { winRate: 0.6, tierScore: 20 }),
        ]}
      />,
    );
    expect(screen.getAllByRole("link")[0]).toHaveAttribute("href", "/brawlers/16000001");

    await userEvent.click(screen.getByRole("button", { name: /승률/ }));

    expect(screen.getAllByRole("link")[0]).toHaveAttribute("href", "/brawlers/16000003");
  });

  it("같은 헤더를 다시 누르면 오름차순으로 바뀐다", async () => {
    render(
      <ComboRankingTable
        items={[
          combo([shelly, colt], { winRate: 0.4 }),
          combo([bull, jessie], { winRate: 0.6 }),
        ]}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: /승률/ }));
    await userEvent.click(screen.getByRole("button", { name: /승률/ }));
    expect(screen.getAllByRole("link")[0]).toHaveAttribute("href", "/brawlers/16000001");
  });

  it("승률 열이 픽률 열보다 앞에 온다", () => {
    render(<ComboRankingTable items={[combo([shelly, colt, nita], { winRate: 0.55, pickRate: 0.1 })]} />);
    const buttons = screen.getAllByRole("button").map((b) => b.textContent ?? "");
    const win = buttons.findIndex((t) => t.startsWith("승률"));
    const pick = buttons.findIndex((t) => t.startsWith("픽률"));
    expect(win).toBeGreaterThanOrEqual(0);
    expect(win).toBeLessThan(pick);
  });
});
