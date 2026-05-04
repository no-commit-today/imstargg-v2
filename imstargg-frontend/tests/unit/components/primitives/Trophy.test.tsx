import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy 컴포넌트", () => {
  it("항상 기본 trophy 아이콘을 사용한다 (player 트로피)", () => {
    render(<Trophy value={5000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_trophy.png");
  });
});
