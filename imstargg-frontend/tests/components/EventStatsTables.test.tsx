import { render, screen } from "@testing-library/react";
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
  it("결과형 통계 행은 모든 통계 셀을 한 줄 responsive grid 에 배치한다", () => {
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
        sort="tier"
        order="DESC"
        selectedBrawlerId={null}
      />,
    );

    const row = screen.getByRole("button", { name: /데미안/ });
    expect(row).toHaveClass(
      "grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px]",
    );
    expect(row).toHaveClass(
      "sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px]",
    );
  });

  it("선택된 결과형 통계 행은 aria-pressed 와 행 강조선으로 표시한다", () => {
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
        sort="tier"
        order="DESC"
        selectedBrawlerId={16000104}
      />,
    );

    const row = screen.getByRole("button", { pressed: true });
    expect(row).toHaveClass("border-l-4", "border-l-[#FFC60A]");
    expect(screen.queryByText("선택됨")).not.toBeInTheDocument();
  });

  it("순위형 통계 행도 좁은 화면에서 압축된 한 줄 grid 를 사용한다", () => {
    render(
      <EventRankStatsTable
        items={[
          {
            brawler,
            totalBattleCount: 275,
            averageRank: 2.14,
          },
        ]}
        sort="averageRank"
        order="ASC"
        selectedBrawlerId={16000104}
      />,
    );

    const row = screen.getByRole("button", { pressed: true });
    expect(row).toHaveClass("grid-cols-[26px_minmax(0,1fr)_64px_52px]");
    expect(row).toHaveClass("sm:grid-cols-[48px_minmax(0,1fr)_88px_96px]");
  });
});
