import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";

describe("PercentCell", () => {
  it("값을 퍼센트로 렌더한다 (gauge 미지정 시 게이지 없음)", () => {
    const { container } = render(<PercentCell value={0.523} />);
    expect(screen.getByText("52.3%")).toBeInTheDocument();
    expect(container.querySelector(".win-gauge")).toBeNull();
  });

  it("gauge 활성화 시 win-gauge 트랙과 채움이 렌더된다", () => {
    const { container } = render(<PercentCell value={0.6} gauge />);
    const fill = container.querySelector(".win-gauge-fill") as HTMLElement | null;
    expect(fill).not.toBeNull();
    expect(fill?.style.width).toBe("60%");
  });

  it("gauge tone='loss' 일 때 data-tone 속성을 부여한다", () => {
    const { container } = render(<PercentCell value={0.45} gauge gaugeTone="loss" />);
    const fill = container.querySelector(".win-gauge-fill") as HTMLElement;
    expect(fill.getAttribute("data-tone")).toBe("loss");
    expect(fill.style.width).toBe("45%");
  });

  it("100% 초과·음수도 0~100% 로 clamp 한다", () => {
    const { container: c1 } = render(<PercentCell value={1.5} gauge />);
    expect((c1.querySelector(".win-gauge-fill") as HTMLElement).style.width).toBe("100%");
    const { container: c2 } = render(<PercentCell value={-0.2} gauge />);
    expect((c2.querySelector(".win-gauge-fill") as HTMLElement).style.width).toBe("0%");
  });
});
