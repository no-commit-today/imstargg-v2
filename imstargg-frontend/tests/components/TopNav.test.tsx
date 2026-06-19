import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TopNav from "@/components/layout/TopNav";

const push = vi.fn();
let pathname = "/";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  usePathname: () => pathname,
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn(), success: vi.fn(), info: vi.fn() },
}));

describe("TopNav", () => {
  beforeEach(() => {
    push.mockReset();
    pathname = "/";
    window.localStorage.clear();
  });

  it("renders primary nav links without a search box", () => {
    render(<TopNav />);

    // 헤더에서 검색을 제거했으므로 검색 입력이 없어야 한다
    expect(
      screen.queryByLabelText("플레이어 이름 또는 태그 입력"),
    ).not.toBeInTheDocument();
    expect(screen.getByText("브롤러")).toBeInTheDocument();
    expect(screen.getByText("이벤트")).toBeInTheDocument();
    expect(screen.getByText("플레이어")).toBeInTheDocument();
    expect(screen.getByText("클럽")).toBeInTheDocument();
  });

  it("opens mobile menu drawer with primary links only (no search)", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));

    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();
    // 헤더·드로어 어디에도 검색 입력이 없다
    expect(
      screen.queryByLabelText("플레이어 이름 또는 태그 입력"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "브롤러 페이지로 이동" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "이벤트 페이지로 이동" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "플레이어 페이지로 이동" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "클럽 페이지로 이동" }),
    ).toBeInTheDocument();
  });
});
