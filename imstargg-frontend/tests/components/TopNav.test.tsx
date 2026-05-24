import { render, screen, waitFor } from "@testing-library/react";
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

  it("renders desktop search and primary nav links", () => {
    render(<TopNav />);

    expect(
      screen.getAllByLabelText("플레이어 이름 또는 태그 입력").length,
    ).toBeGreaterThan(0);
    expect(screen.getByText("브롤러")).toBeInTheDocument();
    expect(screen.getByText("이벤트")).toBeInTheDocument();
    expect(screen.getByText("플레이어")).toBeInTheDocument();
    expect(screen.getByText("클럽")).toBeInTheDocument();
  });

  it("opens mobile menu drawer with search and primary links", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));

    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();
    expect(
      screen.getAllByLabelText("플레이어 이름 또는 태그 입력").length,
    ).toBeGreaterThan(1);
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

  it("drawer search submission navigates to /players/search", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));
    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();

    const inputs = screen.getAllByLabelText("플레이어 이름 또는 태그 입력");
    await user.type(inputs[inputs.length - 1], "야옹이");
    await user.click(
      screen.getAllByRole("button", { name: "플레이어 검색" }).at(-1)!,
    );

    await waitFor(() => {
      expect(push).toHaveBeenCalledWith(
        `/players/search?q=${encodeURIComponent("야옹이")}`,
      );
    });
  });
});
