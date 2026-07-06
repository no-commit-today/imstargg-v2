import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RotationCard } from "@app/events/_components/RotationCard";
import type { EventStatsSummary, RotationEvent } from "@/domain/event/types";

const rotation: RotationEvent = {
  event: { id: 15000026, mode: null, mapName: "골드암 협곡" },
  startTime: "2026-07-01T00:00:00Z",
  endTime: "2099-01-01T00:00:00Z",
};

describe("RotationCard", () => {
  it("summary 가 있으면 판수와 승률 1위 브롤러를 노출한다", () => {
    const summary: EventStatsSummary = {
      totalBattleCount: 12345,
      topBrawlers: [{ brawlerId: 16000000, name: "쉘리", winRate: 0.61 }],
    };
    render(<RotationCard rotation={rotation} summary={summary} />);
    expect(screen.getByText("12,345")).toBeInTheDocument();
    expect(screen.getByText(/쉘리/)).toBeInTheDocument();
    expect(screen.getByText("61.0%")).toBeInTheDocument();
  });

  it("summary 가 없으면 통계 줄을 렌더링하지 않는다", () => {
    render(<RotationCard rotation={rotation} />);
    expect(screen.queryByText(/판 집계/)).not.toBeInTheDocument();
  });
});
