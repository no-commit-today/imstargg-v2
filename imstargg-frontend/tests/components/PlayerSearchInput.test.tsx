import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { addRecent, getRecents } from "@/lib/searchHistory";
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
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.queryByRole("listbox")).toBeNull();
  });

  it("최근 검색이 있으면 포커스 시 드롭다운 노출", () => {
    addRecent({ kind: "player", tag: "#ABC" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.getByRole("listbox")).toBeInTheDocument();
    expect(screen.getByText("#ABC")).toBeInTheDocument();
  });

  it("즐겨찾기가 있으면 즐겨찾기 섹션 노출", () => {
    toggleFavorite({ kind: "player", tag: "#FAV" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.getByText("즐겨찾기")).toBeInTheDocument();
    expect(screen.getByText("#FAV")).toBeInTheDocument();
  });

  it("폼 제출 성공 시 최근 검색에 추가됨", () => {
    render(<PlayerSearchInput />);
    const input = screen.getByLabelText("플레이어 태그 입력") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "#88J8Q9RPV" } });
    fireEvent.submit(input.closest("form")!);
    expect(pushMock).toHaveBeenCalledWith("/players/%2388J8Q9RPV");
    expect(getRecents()[0].tag).toBe("#88J8Q9RPV");
  });

  it("드롭다운 항목 별표 클릭 시 즐겨찾기 토글", () => {
    addRecent({ kind: "player", tag: "#X" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    const starBtn = screen.getByLabelText("즐겨찾기에 추가");
    fireEvent.click(starBtn);
    expect(screen.getAllByLabelText("즐겨찾기 제거").length).toBeGreaterThan(0);
  });
});
