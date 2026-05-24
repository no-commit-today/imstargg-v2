import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EventResultStatsTable } from "@app/events/[id]/_components/EventResultStatsTable";
import { EventRankStatsTable } from "@app/events/[id]/_components/EventRankStatsTable";
import type { Brawler } from "@/domain/brawler/types";

const nav = vi.hoisted(() => ({
  replace: vi.fn(),
  searchParams: new URLSearchParams(""),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: nav.replace }),
  usePathname: () => "/events/15001021",
  useSearchParams: () => nav.searchParams,
}));

const brawler: Brawler = {
  id: 16000104,
  name: "데미안",
  rarity: "EPIC",
  role: "TANK",
  portraitImageUrl: null,
};

describe("EventStatsTables", () => {
  it("결과형 통계 행은 승률·픽률·스타플레이어·전투수 셀을 모두 노출한다", () => {
    render(
      <EventResultStatsTable
        items={[
          {
            brawler,
            totalBattleCount: 275,
            pickRate: 0.03,
            winRate: 0.713,
            starPlayerRate: 0.276,
            tierScore: 100,
            tier: "S",
          },
        ]}
        selectedBrawlerId={null}
      />,
    );

    const row = screen.getByRole("button", { name: /데미안/ });
    expect(within(row).getByText("71.3%")).toBeInTheDocument();
    expect(within(row).getByText("3.0%")).toBeInTheDocument();
    expect(within(row).getByText("27.6%")).toBeInTheDocument();
    expect(within(row).getByText("275")).toBeInTheDocument();
  });

  it("선택된 결과형 통계 행은 aria-pressed=true 로 표시된다", () => {
    render(
      <EventResultStatsTable
        items={[
          {
            brawler,
            totalBattleCount: 275,
            pickRate: 0.03,
            winRate: 0.713,
            starPlayerRate: 0.276,
            tierScore: 100,
            tier: "S",
          },
        ]}
        selectedBrawlerId={16000104}
      />,
    );

    expect(screen.getByRole("button", { pressed: true })).toHaveAccessibleName(
      /데미안/,
    );
  });

  it("순위형 통계 행은 평균순위·전투수 셀을 노출하고 선택 상태를 aria-pressed 로 알린다", () => {
    render(
      <EventRankStatsTable
        items={[
          {
            brawler,
            totalBattleCount: 275,
            averageRank: 2.14,
          },
        ]}
        selectedBrawlerId={16000104}
      />,
    );

    const row = screen.getByRole("button", { pressed: true });
    expect(within(row).getByText("2.14")).toBeInTheDocument();
    expect(within(row).getByText("275")).toBeInTheDocument();
  });
});
