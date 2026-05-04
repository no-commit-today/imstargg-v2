import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy 컴포넌트", () => {
  it("1000 미만은 기본 trophy 아이콘을 사용한다", () => {
    const { container } = render(<Trophy value={999} />);
    const img = container.querySelector("img");
    expect(img).toHaveAttribute("src", "/assets/icon_trophy.png");
  });

  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다", () => {
    const { container } = render(<Trophy value={1000} />);
    const img = container.querySelector("img");
    expect(img).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });

  it("3000 이상은 prestige_trophy 를 사용한다", () => {
    const { container } = render(<Trophy value={3000} />);
    const img = container.querySelector("img");
    expect(img).toHaveAttribute("src", "/assets/icon_prestige_trophy.webp");
  });
});
