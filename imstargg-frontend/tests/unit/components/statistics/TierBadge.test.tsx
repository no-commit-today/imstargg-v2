import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TierBadge } from "@app/_components/statistics/TierBadge";

describe("TierBadge", () => {
  it("S 티어를 렌더한다", () => {
    render(<TierBadge tier="S" />);
    expect(screen.getByText("S")).toBeInTheDocument();
  });

  it("D 티어를 렌더한다", () => {
    render(<TierBadge tier="D" />);
    expect(screen.getByText("D")).toBeInTheDocument();
  });

  it("aria-label 에 tier 가 포함된다", () => {
    render(<TierBadge tier="A" />);
    expect(screen.getByLabelText(/A 티어/)).toBeInTheDocument();
  });
});
