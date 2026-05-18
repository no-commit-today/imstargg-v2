import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderToString } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TopNav from "@/components/layout/TopNav";

const push = vi.fn();
let pathname = "/";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  usePathname: () => pathname,
}));

describe("TopNav", () => {
  beforeEach(() => {
    push.mockReset();
    pathname = "/";
    window.localStorage.clear();
  });

  it("opens mobile menu drawer with search and primary links", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));

    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();
    expect(screen.getAllByPlaceholderText("#PLAYERTAG").length).toBeGreaterThan(
      0,
    );
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

  it("submits drawer search and closes the drawer", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));
    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();
    const inputs = screen.getAllByPlaceholderText("#PLAYERTAG");
    await user.type(inputs[inputs.length - 1], "#88J8Q9RPV");
    await user.click(
      screen.getAllByRole("button", { name: "플레이어 검색" }).at(-1)!,
    );

    expect(push).toHaveBeenCalledWith("/players/88J8Q9RPV");
    await waitFor(() => {
      expect(
        screen.queryByRole("dialog", { name: "메뉴" }),
      ).not.toBeInTheDocument();
    });
  });

  it("syncs player paths into recents after render without React render-phase warnings", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    pathname = "/players/88J8Q9RPV";

    renderToString(<TopNav />);
    expect(window.localStorage.getItem("imstargg.recentSearches")).toBeNull();

    render(<TopNav />);

    await waitFor(() => {
      const recents = JSON.parse(
        window.localStorage.getItem("imstargg.recentSearches") ?? "[]",
      );
      expect(recents[0]?.tag).toBe("88J8Q9RPV");
    });

    expect(
      consoleError.mock.calls.some((call) =>
        String(call[0]).includes("Cannot update a component"),
      ),
    ).toBe(false);
    consoleError.mockRestore();
  });
});
