import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PlayerHero } from "@app/players/[tag]/_components/PlayerHero";
import type { Player } from "@/domain/player/types";

const player: Player = {
  tag: "#88J8Q9RPV",
  name: "惡鬼滅殺",
  nameColor: null,
  iconId: 28000010,
  trophies: 34679,
  highestTrophies: 34679,
  expLevel: 150,
  expPoints: 116278,
  victories3vs3: 7286,
  victoriesSolo: 321,
  victoriesDuo: 184,
  totalPrestigeLevel: 2,
  club: { tag: "#SJCE", name: "SJCE" },
};

function wrap(ui: ReactNode) {
  const qc = new QueryClient();
  return <QueryClientProvider client={qc}>{ui}</QueryClientProvider>;
}

describe("PlayerHero", () => {
  it("이름·태그·4 stats·클럽을 모두 노출한다", () => {
    render(wrap(<PlayerHero player={player} />));
    expect(screen.getByRole("heading", { name: /惡鬼滅殺/ })).toBeInTheDocument();
    expect(screen.getByText("#88J8Q9RPV")).toBeInTheDocument();
    expect(screen.getAllByText("34,679").length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText("7,286")).toBeInTheDocument();
    expect(screen.getByText("505")).toBeInTheDocument();
    expect(screen.getByText(/SJCE/)).toBeInTheDocument();
  });

  it("club null 이면 '소속 클럽 없음' 을 노출한다", () => {
    const noClub: Player = { ...player, club: null };
    render(wrap(<PlayerHero player={noClub} />));
    expect(screen.getByText("소속 클럽 없음")).toBeInTheDocument();
  });
});
