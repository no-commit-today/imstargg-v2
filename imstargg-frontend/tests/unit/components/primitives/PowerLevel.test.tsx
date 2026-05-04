import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PowerLevel from "@/components/primitives/PowerLevel";

describe("PowerLevel", () => {
  it("base SVG 위에 숫자를 표시한다", () => {
    render(<PowerLevel level={11} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/power_level_base_empty.svg");
    expect(screen.getByLabelText("파워 레벨 11")).toBeInTheDocument();
  });
});
