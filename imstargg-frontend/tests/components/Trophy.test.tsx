import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy", () => {
  it("값을 ko-KR 천단위 구분으로 포맷한다", () => {
    render(<Trophy value={34679} />);
    expect(screen.getByText("34,679")).toBeInTheDocument();
  });

  it("icon_trophy.png 이미지를 렌더한다", () => {
    const { container } = render(<Trophy value={1000} size={20} />);
    const img = container.querySelector("img");
    expect(img?.getAttribute("src")).toBe("/assets/icon_trophy.png");
    expect(img?.getAttribute("alt")).toBe("");
  });
});
