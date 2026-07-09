import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeAll, describe, expect, it, vi } from "vitest";
import {
  StatRangeSelector,
  StatRangeSelectorView,
} from "@app/_components/statistics/StatRangeSelector";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/brawlers",
  useSearchParams: () => new URLSearchParams(""),
}));

// Radix Select 가 jsdom 에 없는 Pointer Capture API 를 요구한다
beforeAll(() => {
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

describe("StatRangeSelector (목록 페이지 래퍼)", () => {
  it("현재 tier/date 라벨을 표시한다", () => {
    render(<StatRangeSelector tier="TROPHY_2000_PLUS" date="LAST_WEEK" />);
    expect(screen.getByText("트로피 2,000+")).toBeInTheDocument();
    expect(screen.getByText("지난 주")).toBeInTheDocument();
  });
});

describe("StatRangeSelectorView (controlled)", () => {
  it("티어 선택 시 onSelect 로 새 필터를 전달한다", async () => {
    const onSelect = vi.fn();
    render(
      <StatRangeSelectorView
        tier="TROPHY_1000_PLUS"
        date="SINCE_LAST_WEEK"
        onSelect={onSelect}
      />,
    );
    await userEvent.click(screen.getByRole("combobox", { name: "티어 범위 선택" }));
    await userEvent.click(await screen.findByRole("option", { name: /트로피 3,000\+/ }));
    expect(onSelect).toHaveBeenCalledWith({
      tier: "TROPHY_3000_PLUS",
      date: "SINCE_LAST_WEEK",
    });
  });
});
