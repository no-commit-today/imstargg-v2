import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { EventHero } from "@app/events/[id]/_components/EventHero";
import type { BattleEvent, BattleEventMode } from "@/domain/event/types";

const event = (resultType: BattleEventMode["resultType"]): BattleEvent => ({
  id: 15000026,
  mode: { id: 0, name: "젬 그랩", iconImageUrl: null, resultType },
  mapName: "광산 분쟁",
});

describe("EventHero", () => {
  it("resultType 이 RESULT 면 결과형 배지를 보인다", () => {
    render(<EventHero event={event("RESULT")} />);
    expect(screen.getByText(/결과형/)).toBeInTheDocument();
  });

  it("resultType 이 RANK 면 순위형 배지를 보인다", () => {
    render(<EventHero event={event("RANK")} />);
    expect(screen.getByText("순위형")).toBeInTheDocument();
  });

  it("resultType 이 null 이면 배지를 보이지 않는다", () => {
    const { container } = render(<EventHero event={event(null)} />);
    expect(screen.getByRole("heading", { name: "광산 분쟁" })).toBeInTheDocument();
    expect(screen.queryByText(/결과형|순위형/)).not.toBeInTheDocument();
    // 문구만 가리면 빈 배지가 남아도 통과하므로 배지 요소 자체가 없음을 함께 확인한다
    expect(container.querySelector('[data-slot="badge"]')).toBeNull();
  });
});
