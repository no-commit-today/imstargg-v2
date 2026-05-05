import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PrestigeBadge from "@/components/primitives/PrestigeBadge";

describe("PrestigeBadge", () => {
  it.each([
    [0, "/assets/icon_trophy_brawler_wood.webp", null],
    [249, "/assets/icon_trophy_brawler_wood.webp", null],
    [250, "/assets/icon_trophy_brawler_bronze.webp", null],
    [500, "/assets/icon_trophy_brawler_silver.webp", null],
    [750, "/assets/icon_trophy_brawler_gold.webp", null],
    [1000, "/assets/icon_trophy_brawler_prestige_1.webp", "1"],
    [2000, "/assets/icon_trophy_brawler_prestige_2.webp", "2"],
    [3000, "/assets/icon_trophy_brawler_prestige_3.webp", "3"],
  ] as const)("trophies=%d 에서 올바른 아이콘과 오버레이를 렌더한다", (trophies, src, overlay) => {
    render(<PrestigeBadge trophies={trophies} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", src);
    if (overlay) {
      expect(screen.getByText(overlay)).toBeInTheDocument();
    }
  });
});
