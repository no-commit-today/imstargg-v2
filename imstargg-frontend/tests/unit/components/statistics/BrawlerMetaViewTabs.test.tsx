import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { BrawlerMetaViewTabs } from "@app/brawlers/_components/BrawlerMetaViewTabs";

const { replaceMock } = vi.hoisted(() => ({ replaceMock: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
  usePathname: () => "/brawlers",
  useSearchParams: () => new URLSearchParams("tier=TROPHY_2000_PLUS"),
}));

// Radix 가 jsdom 에 없는 Pointer Capture API 를 요구할 수 있다
beforeAll(() => {
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

beforeEach(() => {
  replaceMock.mockClear();
});

describe("BrawlerMetaViewTabs", () => {
  it("세 탭을 라벨대로 그리고 현재 view 를 선택 상태로 표시한다", () => {
    render(<BrawlerMetaViewTabs view="pair" />);
    expect(screen.getByRole("tab", { name: "브롤러" })).toHaveAttribute("aria-selected", "false");
    expect(screen.getByRole("tab", { name: "2인 조합" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tab", { name: "3인 조합" })).toHaveAttribute("aria-selected", "false");
  });

  it("'2인 조합' 탭을 누르면 ?tier=TROPHY_2000_PLUS&view=pair 로 replace 한다", async () => {
    render(<BrawlerMetaViewTabs view="brawler" />);
    await userEvent.click(screen.getByRole("tab", { name: "2인 조합" }));
    expect(replaceMock).toHaveBeenCalledWith("/brawlers?tier=TROPHY_2000_PLUS&view=pair", {
      scroll: false,
    });
  });

  it("'브롤러' 탭은 view 를 지운다", async () => {
    render(<BrawlerMetaViewTabs view="pair" />);
    await userEvent.click(screen.getByRole("tab", { name: "브롤러" }));
    expect(replaceMock).toHaveBeenCalledWith("/brawlers?tier=TROPHY_2000_PLUS", {
      scroll: false,
    });
  });
});
