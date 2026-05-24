import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";

describe("PercentCell", () => {
  it("값을 퍼센트로 렌더한다", () => {
    render(<PercentCell value={0.523} />);
    expect(screen.getByText("52.3%")).toBeInTheDocument();
  });

  it("fractionDigits 로 소수 자릿수를 조절할 수 있다", () => {
    render(<PercentCell value={0.5234} fractionDigits={2} />);
    expect(screen.getByText("52.34%")).toBeInTheDocument();
  });
});
