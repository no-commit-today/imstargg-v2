import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/brawlers/1",
  useSearchParams: () => new URLSearchParams(""),
}));

describe("SortableHeader", () => {
  it("활성 컬럼은 ▼ 표시", () => {
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    expect(screen.getByRole("button")).toHaveTextContent("▼");
  });

  it("비활성 컬럼은 화살표 없음", () => {
    render(
      <SortableHeader
        column="pickRate"
        label="픽률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    expect(screen.getByRole("button")).not.toHaveTextContent("▼");
    expect(screen.getByRole("button")).not.toHaveTextContent("▲");
  });

  it("활성 컬럼 클릭 시 order 토글", () => {
    replace.mockClear();
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(replace).toHaveBeenCalledWith(expect.stringContaining("order=ASC"), expect.any(Object));
  });
});
