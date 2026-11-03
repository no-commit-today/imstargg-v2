import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RotationCard } from "@app/events/_components/RotationCard";
import type { RotationEvent } from "@/domain/event/types";

const rotation: RotationEvent = {
  event: { id: 15000026, mode: null, mapName: "골드암 협곡" },
  startTime: "2026-07-01T00:00:00Z",
  endTime: "2099-01-01T00:00:00Z",
};

describe("RotationCard", () => {
  it("맵 이름과 이벤트 상세 링크를 노출한다", () => {
    render(<RotationCard rotation={rotation} />);
    expect(screen.getByText("골드암 협곡")).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", "/events/15000026");
  });

  it("mode 가 없으면 기본 모드 라벨을 노출한다", () => {
    render(<RotationCard rotation={rotation} />);
    expect(screen.getByText("이벤트")).toBeInTheDocument();
  });

  it("젬그랩(modeId 0)이면 카드 머리가 보라 배경이다", async () => {
    // modeId 0 은 falsy — 호출부가 event.mode?.id 를 넘기지 않거나 truthiness 로 거르면 회색이 된다
    const gemGrab: RotationEvent = {
      ...rotation,
      event: {
        ...rotation.event,
        mode: { id: 0, name: "젬 그랩", iconImageUrl: null, resultType: "RESULT" },
      },
    };
    render(<RotationCard rotation={gemGrab} />);
    // findBy 는 RTL asyncWrapper 가 act 환경(IS_REACT_ACT_ENVIRONMENT)을 끄고 기다리므로 EventCountdown 의 마운트 직후 상태 갱신이 act 경고를 내지 않는다
    expect((await screen.findByText("젬 그랩")).closest(".event-card-head")).toHaveStyle({
      background: "#8F43EA",
    });
  });
});
