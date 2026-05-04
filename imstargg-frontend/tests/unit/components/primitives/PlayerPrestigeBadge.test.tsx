import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PlayerPrestigeBadge from "@/components/primitives/PlayerPrestigeBadge";

describe("PlayerPrestigeBadge", () => {
  it("level=0 이면 아무것도 렌더하지 않는다", () => {
    const { container } = render(<PlayerPrestigeBadge level={0} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("level>=1 이면 prestige 아이콘 + 숫자를 렌더한다", () => {
    render(<PlayerPrestigeBadge level={5} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_total_prestige.webp",
    );
    expect(screen.getByLabelText("프레스티지 5")).toBeInTheDocument();
  });
});
