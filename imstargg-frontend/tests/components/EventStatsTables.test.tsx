import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EventResultStatsTable } from "@app/events/[id]/_components/EventResultStatsTable";
import { EventRankStatsTable } from "@app/events/[id]/_components/EventRankStatsTable";
import type { Brawler } from "@/domain/brawler/types";

const brawler: Brawler = {
  id: 16000104,
  name: "데미안",
  rarity: "EPIC",
  role: "TANK",
  portraitImageUrl: null,
};

const resultItem = {
  battleCount: 275,
  pickRate: 0.03,
  winRate: 0.713,
  starPlayerRate: 0.276,
  tierScore: 100,
  tier: "S" as const,
};

describe("EventStatsTables", () => {
  it("결과형 통계 행은 승률·픽률·스타플레이어·전투수 셀을 모두 노출한다", () => {
    render(<EventResultStatsTable items={[{ brawler, ...resultItem }]} />);

    const row = screen.getByRole("link", { name: /데미안/ });
    expect(within(row).getByText("71.3%")).toBeInTheDocument();
    expect(within(row).getByText("3.0%")).toBeInTheDocument();
    expect(within(row).getByText("27.6%")).toBeInTheDocument();
    expect(within(row).getByText("275")).toBeInTheDocument();
  });

  it("순위형 통계 행은 평균순위·전투수 셀을 노출한다", () => {
    render(
      <EventRankStatsTable
        items={[{ brawler, totalBattleCount: 275, averageRank: 2.14 }]}
      />,
    );

    const row = screen.getByRole("link", { name: /데미안/ });
    expect(within(row).getByText("2.14")).toBeInTheDocument();
    expect(within(row).getByText("275")).toBeInTheDocument();
  });

  it("결과형 행은 브롤러 상세로 가는 링크다", () => {
    render(<EventResultStatsTable items={[{ brawler, ...resultItem }]} />);

    expect(screen.getByRole("link", { name: /데미안/ })).toHaveAttribute(
      "href",
      "/brawlers/16000104",
    );
  });

  it("순위형 행은 브롤러 상세로 가는 링크다", () => {
    render(
      <EventRankStatsTable
        items={[{ brawler, totalBattleCount: 275, averageRank: 2.14 }]}
      />,
    );

    expect(screen.getByRole("link", { name: /데미안/ })).toHaveAttribute(
      "href",
      "/brawlers/16000104",
    );
  });

  it("브롤러가 없는 행은 링크 없이 그대로 표시된다", () => {
    render(
      <>
        <EventResultStatsTable items={[{ brawler: null, ...resultItem }]} />
        <EventRankStatsTable
          items={[{ brawler: null, totalBattleCount: 10, averageRank: 3 }]}
        />
      </>,
    );

    expect(screen.queryByRole("link")).toBeNull();
    expect(screen.getAllByText("-")).toHaveLength(2);
  });
});
