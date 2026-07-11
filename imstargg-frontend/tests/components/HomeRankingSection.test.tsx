import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeRankingSection } from "@app/_components/home/HomeRankingSection";
import type { PlayerRanking } from "@/domain/ranking/types";

function ranking(rank: number, tag: string, name: string): PlayerRanking {
  return {
    country: "global",
    rank,
    player: { tag, name, nameColor: null, iconId: 28000000, clubName: "" },
    trophies: 100000 - rank,
    rankChange: null,
  };
}

describe("HomeRankingSection", () => {
  it("상위 10명만 렌더링하고 플레이어 페이지로 링크한다", () => {
    const rankings = Array.from({ length: 15 }, (_, i) =>
      ranking(i + 1, `#TAG${i}`, `플레이어${i}`),
    );
    render(<HomeRankingSection rankings={rankings} />);
    expect(screen.getAllByRole("listitem")).toHaveLength(10);
    expect(screen.getByRole("link", { name: /플레이어0/ })).toHaveAttribute(
      "href",
      "/players/%23TAG0",
    );
    expect(screen.getByText("99,999")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/players",
    );
  });

  it("빈 배열이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<HomeRankingSection rankings={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
