import { beforeAll, beforeEach, describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { server } from "../setup/msw";
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
// DTO 브롤러 — name 은 MessagesResponse ({ ko })
const comboDto = { id: 16000200, name: { ko: "콜트" }, rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const selfDto = { id: 16000104, name: { ko: "데미안" }, rarity: "EPIC", role: "TANK", portraitImageUrl: null };

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

  it("브롤러 행 클릭 → 선택 패널이 열리고 URL 에 brawlerId 가 실린다", async () => {
    server.use(
      http.get("*/v1/statistics/brawler/pair/result", () =>
        HttpResponse.json({
          content: [
            { brawler: selfDto, pairBrawler: comboDto, battleCount: 10, pickRate: 0.1, winRate: 0.5 },
          ],
        }),
      ),
      http.get("*/v1/statistics/brawler/enemy/result", () =>
        HttpResponse.json({ content: [] }),
      ),
    );

    renderWithProviders(
      <EventStatsPanel
        eventId={15001021}
        resultType="RESULT"
        initialResult={initialResult}
        initialRank={null}
      />,
    );

    await userEvent.click(await screen.findByRole("button", { name: /데미안/ }));
    expect(await screen.findByText("선택한 브롤러 분석")).toBeInTheDocument();
    expect(window.location.search).toContain("brawlerId=16000104");

    await userEvent.click(screen.getByRole("button", { name: /닫기/ }));
    expect(screen.queryByText("선택한 브롤러 분석")).not.toBeInTheDocument();
    expect(window.location.search).not.toContain("brawlerId");
  });
});
