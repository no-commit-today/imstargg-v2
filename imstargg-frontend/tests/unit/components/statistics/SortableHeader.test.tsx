import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";

describe("SortableHeader", () => {
  it("활성 컬럼은 ▼ 표시", () => {
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
        onSort={() => {}}
      />,
    );
    expect(screen.getByRole("button")).toHaveTextContent("▼");
  });

  it("비활성 컬럼은 화살표 없음", () => {
    render(
      <SortableHeader<"winRate" | "pickRate">
        column="pickRate"
        label="픽률"
        currentSort="winRate"
        currentOrder="DESC"
        onSort={() => {}}
      />,
    );
    expect(screen.getByRole("button")).not.toHaveTextContent("▼");
    expect(screen.getByRole("button")).not.toHaveTextContent("▲");
  });

  it("클릭 시 컬럼 인자와 함께 onSort 가 호출된다", () => {
    const onSort = vi.fn();
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
        onSort={onSort}
      />,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(onSort).toHaveBeenCalledWith("winRate");
  });

  it("활성 컬럼은 data-active='true' 속성을 가진다", () => {
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
        onSort={() => {}}
      />,
    );
    expect(screen.getByRole("button").getAttribute("data-active")).toBe("true");
  });
});
