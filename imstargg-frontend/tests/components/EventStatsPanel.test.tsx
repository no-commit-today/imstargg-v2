import { beforeAll, beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../setup/renderWithProviders";
import { EventStatsPanel } from "@app/events/[id]/_components/EventStatsPanel";
import type { BrawlerResultStatistics } from "@/domain/statistics/types";
import type { Brawler } from "@/domain/brawler/types";

beforeAll(() => {
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

beforeEach(() => {
  window.history.replaceState(null, "", "/events/15001021");
});

// 도메인(매핑 후) Brawler — name 은 평문 string
const demian: Brawler = { id: 16000104, name: "데미안", rarity: "EPIC", role: "TANK", portraitImageUrl: null };

const initialResult: BrawlerResultStatistics = {
  totalBattleCount: 1000,
  items: [
    {
      brawler: demian,
      battleCount: 275,
      pickRate: 0.03,
      winRate: 0.713,
      starPlayerRate: 0.276,
      tierScore: 100,
      tier: "S",
    },
  ],
};

describe("EventStatsPanel (결과형)", () => {
  it("초기 통계를 렌더한다", async () => {
    renderWithProviders(
      <EventStatsPanel
        eventId={15001021}
        resultType="RESULT"
        initialResult={initialResult}
        initialRank={null}
      />,
    );
    expect(await screen.findByText("데미안")).toBeInTheDocument();
  });

  it("통계 행은 브롤러 상세 링크다", async () => {
    renderWithProviders(
      <EventStatsPanel
        eventId={15001021}
        resultType="RESULT"
        initialResult={initialResult}
        initialRank={null}
      />,
    );

    const link = await screen.findByRole("link", { name: /데미안/ });
    expect(link).toHaveAttribute("href", "/brawlers/16000104");
    expect(screen.queryByRole("button", { pressed: false })).toBeNull();
  });
});
