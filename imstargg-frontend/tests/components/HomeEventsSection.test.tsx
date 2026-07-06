import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeEventsSection } from "@app/_components/home/HomeEventsSection";
import type { RotationEvent } from "@/domain/event/types";

function rotation(id: number, mapName: string): RotationEvent {
  return {
    event: { id, mode: null, mapName },
    startTime: "2026-07-01T00:00:00Z",
    endTime: "2099-01-01T00:00:00Z",
  };
}

describe("HomeEventsSection", () => {
  it("최대 6개 이벤트 카드를 렌더링하고 이벤트 페이지로 링크한다", () => {
    const events = Array.from({ length: 8 }, (_, i) =>
      rotation(15000000 + i, `맵${i}`),
    );
    render(<HomeEventsSection rotation={events} />);
    expect(screen.getByText("맵0")).toBeInTheDocument();
    expect(screen.queryByText("맵7")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /맵0/ })).toHaveAttribute(
      "href",
      "/events/15000000",
    );
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/events",
    );
  });

  it("빈 배열이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<HomeEventsSection rotation={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
