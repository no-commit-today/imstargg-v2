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
});
