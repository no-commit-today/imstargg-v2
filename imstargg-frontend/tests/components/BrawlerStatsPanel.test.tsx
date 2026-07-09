import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { BrawlerStatsPanel } from "@app/brawlers/[id]/_components/BrawlerStatsPanel";
import type { Brawler } from "@/domain/brawler/types";

beforeAll(() => {
  // recharts(ChartContainer)가 jsdom 에서 요구하는 ResizeObserver 스텁
  vi.stubGlobal(
    "ResizeObserver",
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  );
  // Radix Select 가 jsdom 에 없는 Pointer Capture API 를 요구한다
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

beforeEach(() => {
  // 테스트 간 URL(쿼리스트링) 오염 방지 — 패널이 mount 시 URL 을 읽는다
  window.history.replaceState(null, "", "/brawlers/16000000");
});

const self: Brawler = { id: 16000000, name: "브롤러", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const shelly: Brawler = { id: 16000001, name: "쉘리", rarity: "EPIC", role: "TANK", portraitImageUrl: null };
const colt: Brawler = { id: 16000002, name: "콜트", rarity: "EPIC", role: "TANK", portraitImageUrl: null };

const initial = {
  daily: [],
  pair: [
    { brawler: self, pairBrawler: shelly, battleCount: 100, pickRate: 0.1, winRate: 0.55 },
  ],
  enemy: [],
};

describe("BrawlerStatsPanel", () => {
  it("서버 initial 데이터를 즉시 렌더한다 (조합 브롤러명 노출)", async () => {
    renderWithProviders(<BrawlerStatsPanel brawlerId={16000000} initial={initial} />);
    expect(await screen.findByText("쉘리")).toBeInTheDocument();
  });

  it("티어 필터 변경 시 해당 필터로 통계를 다시 가져오고 URL 을 갱신한다", async () => {
    let requestedTier: string | null = null;
    server.use(
      http.get("*/v1/statistics/brawler/result/daily", () => HttpResponse.json({ content: [] })),
      http.get("*/v1/statistics/brawler/pair/result", ({ request }) => {
        requestedTier = new URL(request.url).searchParams.get("tierRange");
        // MSW 는 raw DTO 를 반환해야 한다 — BrawlerResponse.name 은
        // string 이 아니라 MessagesResponse(Record<Language, string>).
        return HttpResponse.json({
          content: [
            {
              brawler: { ...self, name: { ko: self.name } },
              pairBrawler: { ...colt, name: { ko: colt.name } },
              battleCount: 42,
              pickRate: 0.1,
              winRate: 0.61,
            },
          ],
        });
      }),
      http.get("*/v1/statistics/brawler/enemy/result", () => HttpResponse.json({ content: [] })),
    );

    renderWithProviders(<BrawlerStatsPanel brawlerId={16000000} initial={initial} />);

    await userEvent.click(screen.getByRole("combobox", { name: "티어 범위 선택" }));
    await userEvent.click(await screen.findByRole("option", { name: /트로피 3,000\+/ }));

    expect(await screen.findByText("콜트")).toBeInTheDocument();
    await waitFor(() => expect(requestedTier).toBe("TROPHY_3000_PLUS"));
    expect(window.location.search).toContain("tier=TROPHY_3000_PLUS");
  });
});
