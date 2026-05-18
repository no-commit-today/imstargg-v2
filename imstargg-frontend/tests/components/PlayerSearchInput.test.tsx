import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { addRecent } from "@/lib/searchHistory";
import { toggleFavorite } from "@/lib/favorites";

const pushMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn(), success: vi.fn(), info: vi.fn() },
}));

describe("PlayerSearchInput", () => {
  beforeEach(() => {
    localStorage.clear();
    pushMock.mockClear();
  });

  it("초기 상태에서 포커스해도 빈 드롭다운은 안 보임", () => {
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 이름 또는 태그 입력"));
    expect(screen.queryByRole("menu")).toBeNull();
  });

  it("최근 검색이 있으면 포커스 시 드롭다운 노출", () => {
    addRecent({ kind: "player", tag: "#ABC", name: "야옹이", trophies: 50000 });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 이름 또는 태그 입력"));
    expect(screen.getByRole("menu")).toBeInTheDocument();
    expect(screen.getByText("#ABC")).toBeInTheDocument();
  });

  it("즐겨찾기가 있으면 즐겨찾기 섹션 노출", () => {
    toggleFavorite({ kind: "player", tag: "#FAV", name: "즐찾이", trophies: 10000 });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 이름 또는 태그 입력"));
    expect(screen.getByText("즐겨찾기")).toBeInTheDocument();
    expect(screen.getByText("#FAV")).toBeInTheDocument();
  });

  it("폼 제출 시 검색 결과 페이지로 이동", () => {
    render(<PlayerSearchInput />);
    const input = screen.getByLabelText("플레이어 이름 또는 태그 입력") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "야옹이" } });
    fireEvent.submit(input.closest("form")!);
    expect(pushMock).toHaveBeenCalledWith("/players/search?q=%EC%95%BC%EC%98%B9%EC%9D%B4");
  });

  it("드롭다운 항목 별표 클릭 시 즐겨찾기 토글", () => {
    addRecent({ kind: "player", tag: "#X", name: "엑스", trophies: 1000 });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 이름 또는 태그 입력"));
    const starBtn = screen.getByLabelText("즐겨찾기에 추가");
    fireEvent.click(starBtn);
    expect(screen.getAllByLabelText("즐겨찾기 제거").length).toBeGreaterThan(0);
  });

  it("최근 항목 링크가 올바른 href 를 가짐", () => {
    addRecent({ kind: "player", tag: "#XYZ", name: "엑스와이지", trophies: 5000 });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 이름 또는 태그 입력"));
    const link = screen.getByRole("menuitem", { name: /XYZ/ });
    // SearchHistoryDropdown 은 routes.player(tag) → toUrlPath → %23XYZ 를 사용
    expect(link).toHaveAttribute("href", "/players/%23XYZ");
  });
});
