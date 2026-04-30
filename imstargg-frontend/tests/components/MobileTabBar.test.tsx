import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import MobileTabBar from "@/components/layout/MobileTabBar";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/",
}));

describe("MobileTabBar", () => {
  it("emoji 가 아닌 자산 또는 line icon 으로 5 탭을 렌더한다", () => {
    const { container } = render(<MobileTabBar />);
    const text = container.textContent ?? "";
    for (const emoji of ["🏠", "⭐", "🗓️", "🏆", "🛡️"]) {
      expect(text.includes(emoji)).toBe(false);
    }
    for (const label of ["홈", "브롤러", "이벤트", "랭킹", "클럽"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });

  it("랭킹 탭에 trophy.png 자산을 사용한다", () => {
    const { container } = render(<MobileTabBar />);
    const imgs = Array.from(container.querySelectorAll("img"));
    expect(imgs.some((i) => i.getAttribute("src") === "/assets/trophy.png")).toBe(true);
  });
});
