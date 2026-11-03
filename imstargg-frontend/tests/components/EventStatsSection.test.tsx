import { beforeAll, beforeEach, describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { EventStatsSection } from "@app/events/[id]/_components/EventStatsSection";
import type { BattleEvent, BattleEventMode } from "@/domain/event/types";

const UNSUPPORTED_NOTICE = "이 모드는 통계를 제공하지 않습니다";

const requested = { result: false, rank: false };

beforeAll(() => {
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

beforeEach(() => {
  requested.result = false;
  requested.rank = false;
  window.history.replaceState(null, "", "/events/15001021");
  // 결과형·순위형 통계 요청이 일어났는지 기록한다. 등록되지 않은 요청은 전역 설정이 오류로 만든다.
  server.use(
    http.get("*/v1/statistics/brawler/result", () => {
      requested.result = true;
      return HttpResponse.json({ totalBattleCount: 0, items: [] });
    }),
    http.get("*/v1/statistics/brawler/rank", () => {
      requested.rank = true;
      return HttpResponse.json({ content: [] });
    }),
  );
});

const event = (mode: BattleEventMode | null): BattleEvent => ({
  id: 15001021,
  mode,
  mapName: "광산 분쟁",
});

const mode = (resultType: BattleEventMode["resultType"]): BattleEventMode => ({
  id: 0,
  name: "젬 그랩",
  iconImageUrl: null,
  resultType,
});

describe("EventStatsSection", () => {
  it("resultType 이 null 이면 안내를 보이고 통계를 요청하지 않는다", async () => {
    const ui = await EventStatsSection({ event: event(mode(null)) });
    renderWithProviders(ui);

    expect(screen.getByText(UNSUPPORTED_NOTICE)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /이벤트 통계/ })).not.toBeInTheDocument();
    expect(requested).toEqual({ result: false, rank: false });
  });

  it("mode 가 null 이어도 안내를 보이고 통계를 요청하지 않는다", async () => {
    const ui = await EventStatsSection({ event: event(null) });
    renderWithProviders(ui);

    expect(screen.getByText(UNSUPPORTED_NOTICE)).toBeInTheDocument();
    expect(requested).toEqual({ result: false, rank: false });
  });

  it("resultType 이 RESULT 면 결과형 통계를 요청한다", async () => {
    const ui = await EventStatsSection({ event: event(mode("RESULT")) });
    renderWithProviders(ui);

    expect(screen.getByRole("heading", { name: "이벤트 통계" })).toBeInTheDocument();
    expect(screen.queryByText(UNSUPPORTED_NOTICE)).not.toBeInTheDocument();
    expect(requested).toEqual({ result: true, rank: false });
  });

  it("resultType 이 RANK 면 순위형 통계를 요청한다", async () => {
    const ui = await EventStatsSection({ event: event(mode("RANK")) });
    renderWithProviders(ui);

    expect(screen.getByRole("heading", { name: "이벤트 통계 (순위형)" })).toBeInTheDocument();
    expect(screen.queryByText(UNSUPPORTED_NOTICE)).not.toBeInTheDocument();
    expect(requested).toEqual({ result: false, rank: true });
  });
});
