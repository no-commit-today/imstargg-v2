import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { BattleList } from "@app/players/[tag]/_components/battle/BattleList";
import type { SliceResponse, BattleResponse } from "@/api/types";

const emptyInitial: SliceResponse<BattleResponse> = { content: [], hasNext: false };

describe("BattleList — RSC 프리페치 initialData", () => {
  it("initialBattles 가 있으면 스켈레톤·네트워크 없이 즉시 렌더한다", () => {
    // battles fetch 가 일어나면 실패시켜, initialData 미소비 시 에러 UI 로 감지되게 한다
    server.use(http.get("*/v1/players/*/battles", () => HttpResponse.error()));
    renderWithProviders(
      <BattleList tag="#88J8Q9RPV" initialBattles={emptyInitial} battlesFetchedAt={Date.now()} />,
    );
    // isLoading=false (initialData 소비) → 스켈레톤이 아니라 빈 상태가 동기적으로 렌더된다
    expect(screen.getByText(/최근 배틀 기록이 없어요/)).toBeInTheDocument();
    // fetch 가 발생했다면 에러 UI("불러오지 못했어요")가 떴을 것 — 없어야 한다
    expect(screen.queryByText(/불러오지 못했어요/)).not.toBeInTheDocument();
  });
});
