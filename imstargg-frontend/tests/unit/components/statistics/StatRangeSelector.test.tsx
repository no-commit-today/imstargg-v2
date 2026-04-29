import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/brawlers",
  useSearchParams: () => new URLSearchParams(""),
}));

describe("StatRangeSelector", () => {
  it("현재 tier/date 라벨을 표시한다", () => {
    render(<StatRangeSelector tier="TROPHY_2000_PLUS" date="LAST_WEEK" />);
    expect(screen.getByText("트로피 2,000+")).toBeInTheDocument();
    expect(screen.getByText("지난 주")).toBeInTheDocument();
  });
});
