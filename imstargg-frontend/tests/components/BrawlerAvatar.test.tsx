import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";

describe("BrawlerAvatar", () => {
  it("imageUrl 이 있으면 alt 가 name 인 이미지를 렌더한다", () => {
    render(
      <BrawlerAvatar
        name="쉘리"
        imageUrl="https://cdn.imstargg.com/brawler/portrait/16000000.webp"
      />,
    );
    const img = screen.getByAltText("쉘리");
    expect(img).toBeInTheDocument();
    expect(img.tagName).toBe("IMG");
    expect(img.getAttribute("src")).toContain("16000000.webp");
  });

  it("imageUrl 이 null 이면 이미지 대신 이니셜을 렌더한다", () => {
    render(<BrawlerAvatar name="쉘리" imageUrl={null} />);
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(screen.getByText("쉘")).toBeInTheDocument();
  });
});
