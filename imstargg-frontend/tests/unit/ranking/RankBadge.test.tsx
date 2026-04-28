import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RankBadge } from "@app/ranking/_components/RankBadge";

describe("RankBadge", () => {
  it("rank를 #N 형식으로 표시한다", () => {
    render(<RankBadge rank={1} rankChange={null} />);
    expect(screen.getByText("#1")).toBeInTheDocument();
  });

  it("rankChange가 양수면 ▲N으로 표시한다", () => {
    render(<RankBadge rank={5} rankChange={3} />);
    expect(screen.getByText("▲3")).toBeInTheDocument();
  });

  it("rankChange가 음수면 ▼N으로 표시한다 (절댓값)", () => {
    render(<RankBadge rank={10} rankChange={-2} />);
    expect(screen.getByText("▼2")).toBeInTheDocument();
  });

  it("rankChange가 0이면 ─로 표시한다", () => {
    render(<RankBadge rank={7} rankChange={0} />);
    expect(screen.getByText("─")).toBeInTheDocument();
  });

  it("rankChange가 null이면 변동 표시가 없다", () => {
    render(<RankBadge rank={2} rankChange={null} />);
    expect(screen.queryByText("▲")).toBeNull();
    expect(screen.queryByText("▼")).toBeNull();
    expect(screen.queryByText("─")).toBeNull();
  });
});
