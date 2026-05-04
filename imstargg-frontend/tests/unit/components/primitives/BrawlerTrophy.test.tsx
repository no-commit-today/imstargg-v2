import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";

describe("BrawlerTrophy 컴포넌트", () => {
  it("1000 미만은 기본 trophy 아이콘을 사용한다", () => {
    render(<BrawlerTrophy value={999} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_trophy.png");
  });
  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다 (하단)", () => {
    render(<BrawlerTrophy value={1000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });
  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다 (상단)", () => {
    render(<BrawlerTrophy value={2999} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });
  it("3000 이상은 prestige_trophy 를 사용한다", () => {
    render(<BrawlerTrophy value={3000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy.webp");
  });
});
