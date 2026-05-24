import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RankNumber } from "@/components/ranking/RankNumber";
import { RankChange } from "@/components/ranking/RankChange";

describe("RankNumber", () => {
  it("rank를 숫자로 표시한다", () => {
    render(<RankNumber rank={1} />);
    expect(screen.getByText("1")).toBeInTheDocument();
  });

  it("top-3에는 메달 스타일을 적용한다", () => {
    const { container } = render(<RankNumber rank={2} />);
    const medal = container.querySelector(".rank-medal");
    expect(medal).not.toBeNull();
    expect(medal?.getAttribute("data-medal")).toBe("silver");
  });

  it("4위 이하는 일반 숫자 스타일을 적용한다", () => {
    const { container } = render(<RankNumber rank={10} />);
    expect(container.querySelector(".rank-number")).not.toBeNull();
    expect(container.querySelector(".rank-medal")).toBeNull();
  });
});

describe("RankChange", () => {
  it("rankChange가 양수면 ▲N으로 표시한다", () => {
    render(<RankChange change={3} />);
    expect(screen.getByText("▲3")).toBeInTheDocument();
  });

  it("rankChange가 음수면 ▼N으로 표시한다 (절댓값)", () => {
    render(<RankChange change={-2} />);
    expect(screen.getByText("▼2")).toBeInTheDocument();
  });

  it("rankChange가 0이면 ─로 표시한다", () => {
    render(<RankChange change={0} />);
    expect(screen.getByText("─")).toBeInTheDocument();
  });

  it("rankChange가 null이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<RankChange change={null} />);
    expect(container.firstChild).toBeNull();
  });
});
