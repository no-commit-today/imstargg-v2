import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeMetaSection } from "@app/_components/home/HomeMetaSection";
import type { BrawlerResultStat } from "@/domain/statistics/types";

function stat(id: number, name: string, winRate: number): BrawlerResultStat {
  return {
    brawler: {
      id,
      name,
      rarity: "STARTING_BRAWLER",
      role: "DAMAGE_DEALER",
      portraitImageUrl: null,
    },
    battleCount: 1000,
    pickRate: 0.05,
    winRate,
    starPlayerRate: 0.1,
    tierScore: winRate,
    tier: "A",
  } as BrawlerResultStat;
}

describe("HomeMetaSection", () => {
  it("승률 내림차순 상위 10개만 렌더링하고 브롤러 페이지로 링크한다", () => {
    const stats = Array.from({ length: 15 }, (_, i) =>
      stat(16000000 + i, `브롤러${i}`, 0.7 - i * 0.01),
    );
    render(<HomeMetaSection stats={stats} />);
    expect(screen.getAllByRole("listitem")).toHaveLength(10);
    expect(screen.getByRole("link", { name: /브롤러0/ })).toHaveAttribute(
      "href",
      "/brawlers/16000000",
    );
    expect(screen.getByText("70.0%")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/brawlers",
    );
  });

  it("brawler 가 null 인 항목은 제외한다", () => {
    const withNull = [{ ...stat(1, "x", 0.5), brawler: null }];
    const { container } = render(<HomeMetaSection stats={withNull} />);
    expect(container).toBeEmptyDOMElement();
  });
});
