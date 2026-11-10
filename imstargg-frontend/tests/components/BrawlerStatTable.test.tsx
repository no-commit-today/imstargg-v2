import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { BrawlerStatTable } from "@app/brawlers/_components/BrawlerStatTable";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";

const legend = /표본 부족: 전투 수가 적어 티어를 매기지 않습니다/;

function brawlerOf(id: number, name: string): Brawler {
  return { id, name, rarity: "RARE", role: "TANK", portraitImageUrl: null };
}

function statOf(
  brawler: Brawler,
  overrides: Partial<BrawlerResultStat>,
): BrawlerResultStat {
  return {
    brawler,
    battleCount: 1000,
    pickRate: 0.05,
    winRate: 0.5,
    starPlayerRate: 0.1,
    tierScore: 50,
    tier: "B",
    ...overrides,
  };
}

const gaga = brawlerOf(16000001, "가가");
const nana = brawlerOf(16000002, "나나");
const dada = brawlerOf(16000003, "다다");

const gagaRow = {
  brawler: gaga,
  stat: statOf(gaga, { tier: "S", tierScore: 62, winRate: 0.56 }),
};
const nanaRow = {
  brawler: nana,
  stat: statOf(nana, { tier: "B", tierScore: 50, winRate: 0.5 }),
};
const dadaRow = {
  brawler: dada,
  stat: statOf(dada, { tier: null, tierScore: 66, winRate: 0.9, battleCount: 100 }),
};

function names() {
  return screen.getAllByRole("link").map((l) => l.textContent ?? "");
}

describe("BrawlerStatTable", () => {
  it("티어 정렬에서 등급 없는 행은 내림차순·오름차순 모두 아래다", async () => {
    const user = userEvent.setup();
    render(<BrawlerStatTable rows={[dadaRow, nanaRow, gagaRow]} totalBattleCount={0} />);

    const order = () => names().map((t) => ["가가", "나나", "다다"].find((n) => t.includes(n)));
    expect(order()).toEqual(["가가", "나나", "다다"]);

    await user.click(screen.getByRole("button", { name: /티어/ }));
    expect(order()).toEqual(["나나", "가가", "다다"]);
  });

  it("등급 없는 행은 승률 1위여도 neutral 레일이다", () => {
    render(
      <BrawlerStatTable
        rows={[gagaRow, nanaRow, dadaRow]}
        totalBattleCount={0}
        initialSort="winRate"
      />,
    );

    expect(screen.getAllByRole("link").map((l) => l.getAttribute("data-rail"))).toEqual([
      "neutral",
      "silver",
      "bronze",
    ]);
  });

  it("통계가 없는 브롤러는 표본 부족 배지다", () => {
    render(<BrawlerStatTable rows={[{ brawler: gaga, stat: null }]} totalBattleCount={0} />);

    expect(screen.getByLabelText("표본 부족")).toBeInTheDocument();
    expect(screen.queryByLabelText(/D 티어/)).toBeNull();
  });

  it("통계가 0건이면 전 브롤러가 표본 부족 배지와 neutral 레일이고 범례가 보인다", () => {
    render(
      <BrawlerStatTable
        rows={[
          { brawler: gaga, stat: null },
          { brawler: nana, stat: null },
          { brawler: dada, stat: null },
        ]}
        totalBattleCount={0}
      />,
    );

    expect(screen.getAllByLabelText("표본 부족")).toHaveLength(3);
    for (const link of screen.getAllByRole("link")) {
      expect(link).toHaveAttribute("data-rail", "neutral");
    }
    expect(screen.getByText(legend)).toBeInTheDocument();
  });

  it("범례는 보이는 행에 등급 없는 행이 있을 때만 그린다", async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <BrawlerStatTable rows={[gagaRow, nanaRow]} totalBattleCount={0} />,
    );
    expect(screen.queryByText(legend)).toBeNull();
    unmount();

    render(<BrawlerStatTable rows={[gagaRow, dadaRow]} totalBattleCount={0} />);
    expect(screen.getByText(legend)).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText("브롤러 이름 검색"), "가가");
    expect(screen.queryByText(legend)).toBeNull();
  });
});
